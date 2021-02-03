//-----------------------------------------------
//
//	This file is part of the Siv3D Engine.
//
//	Copyright (c) 2008-2019 Ryo Suzuki
//	Copyright (c) 2016-2019 OpenSiv3D Project
//
//	Licensed under the MIT License.
//
//-----------------------------------------------

# include <algorithm>
# include <string>
# include <vector>
# include <gio/gio.h>
# include <Siv3D/EngineLog.hpp>
# include <Siv3D/Unicode.hpp>
# include <Siv3D/PowerStatus.hpp>

namespace s3d
{
	namespace detail
	{
		const char* NAME_UPower = "org.freedesktop.UPower";
		const char* OBJECT_UPower = "/org/freedesktop/UPower";
		const char* INTERFACE_UPower = "org.freedesktop.UPower";
		const char* INTERFACE_UPower_Device = "org.freedesktop.UPower.Device";

		bool get_property(GDBusProxy *_proxy, const gchar *_property_name, const gchar *_format, void *_dest)
		{
			GVariant *variant = g_dbus_proxy_get_cached_property(_proxy, _property_name);
			if (variant != nullptr)
			{
				g_variant_get(variant, _format, _dest);
				g_variant_unref(variant);
				return true;
			}
			else
				return false;
		}
		
		bool get_property_and_output_log(GDBusProxy *_proxy, const gchar *_property_name, const gchar *_format, void *_dest)
		{
			bool ret = get_property(_proxy, _property_name, _format, _dest);
			if (ret == false)
			{
				LOG_FAIL(U"❌ PowerStatus: Failed to get UPower device property ({})."_fmt(Unicode::Widen(_property_name)));
			}
			return ret;
		}

		void GetPowerStatus_Linux(PowerStatus& result)
		{
			std::vector<std::string> devicePaths;

			GDBusProxy *proxy;
			GDBusConnection *conn;
			GError *error = nullptr;
			char *s;
			guint32 u;
			guint64 x;
			gboolean b;
			gdouble d;
			GVariant *variant;
			GVariantIter *iter;

			conn = g_bus_get_sync(G_BUS_TYPE_SYSTEM, nullptr, &error);
			if (conn == nullptr)
			{
				LOG_FAIL(U"❌ PowerStatus: Failed to get d-bus connection.");
				return;
			}

			proxy = g_dbus_proxy_new_sync(conn, G_DBUS_PROXY_FLAGS_NONE, nullptr,
				NAME_UPower, OBJECT_UPower, INTERFACE_UPower, nullptr, &error);
			if (proxy == nullptr)
			{
				LOG_FAIL(U"❌ PowerStatus: Failed to get d-bus proxy.");
				return;
			}

			variant = g_dbus_proxy_get_cached_property(proxy, "DaemonVersion");
			if (variant == nullptr)
			{
				LOG_FAIL(U"❌ PowerStatus: Failed to get UPower properties.");
				return;
			}
			g_variant_get(variant, "s", &s);
			g_variant_unref(variant);
			LOG_INFO(U"ℹ️  PowerStatus: UPower daemon version {}"_fmt(Unicode::Widen(s)));

			variant = g_dbus_proxy_call_sync(proxy, "EnumerateDevices", nullptr,
				G_DBUS_CALL_FLAGS_NONE, -1, nullptr, &error);
			if (variant == nullptr)
			{
				LOG_FAIL(U"❌ PowerStatus: Failed to enumerate power supply devices.");
				return;
			}
			g_variant_get(variant, "(ao)", &iter);
			while (g_variant_iter_loop(iter, "o", &s))
			{
				devicePaths.push_back(s);
			}
			g_variant_iter_free(iter);
			g_variant_unref(variant);
			g_object_unref(proxy);

			result.battery = BatteryStatus::NoBattery;

			for (const auto &dev : devicePaths)
			{
				proxy = g_dbus_proxy_new_sync(conn, G_DBUS_PROXY_FLAGS_NONE, nullptr,
					NAME_UPower, dev.c_str(), INTERFACE_UPower_Device, nullptr, &error);

				if (!get_property_and_output_log(proxy, "Type", "u", &u))
					continue;

				if (u == 1)
				{
					//line power
					if (get_property_and_output_log(proxy, "Online", "b", &b))
					{
						result.ac = b ? ACLineStatus::Online : ACLineStatus::Offline;
					}
				}
				else if (u == 2)
				{
					//battery
					if (get_property_and_output_log(proxy, "Percentage", "d", &d))
					{
						result.batteryLifePercent = static_cast<int32>(d);
						result.battery = d <= 5 ? BatteryStatus::Critical
							: d <= 33 ? BatteryStatus::Low
							: d <= 66 ? BatteryStatus::Middle
							: BatteryStatus::High;
					}
					if (get_property_and_output_log(proxy, "State", "u", &u))
					{
						if (u == 1)
						{
							//charging
							result.charging = true;
							if (get_property_and_output_log(proxy, "TimeToFull", "x", &x))
							{
								if (x != 0)
									result.batteryTimeToFullChargeSec = static_cast<int32>(x);
							}
						}
						else if (u == 2)
						{
							//discharging
							if (get_property_and_output_log(proxy, "TimeToEmpty", "x", &x))
							{
								if (x != 0)
									result.batteryLifeTimeSec = static_cast<int32>(x);
							}
						}
					}
				}
			}

			g_object_unref(conn);
		}
	}

	namespace System
	{
		PowerStatus GetPowerStatus()
		{
			PowerStatus status;

			detail::GetPowerStatus_Linux(status);

			return status;
		}
	}
}
