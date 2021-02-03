﻿//-----------------------------------------------
//
//	This file is part of the Siv3D Engine.
//
//	Copyright (c) 2008-2019 Ryo Suzuki
//	Copyright (c) 2016-2019 OpenSiv3D Project
//
//	Licensed under the MIT License.
//
//-----------------------------------------------

# include <Siv3DEngine.hpp>
# include <Texture/ITexture.hpp>
# include <Siv3D/EngineLog.hpp>
# include "CAsset.hpp"

namespace s3d
{
	namespace detail
	{
		constexpr StringView GetAssetTypeName(const AssetType assetType)
		{
			switch (assetType)
			{
			case AssetType::Audio:
				return U"Audio"_sv;
			case AssetType::Texture:
				return U"Texture"_sv;
			case AssetType::Font:
				return U"Font"_sv;
			default:
				return U"Unknown"_sv;
			}
		}
	}

	CAsset::CAsset()
	{

	}

	CAsset::~CAsset()
	{
		LOG_TRACE(U"CAsset::~CAsset()");

		Siv3DEngine::Get<ISiv3DTexture>()->updateAsync(Largest<size_t>);
		
		// wait for all
		for (auto& assetList : m_assetLists)
		{
			for (auto& asset : assetList)
			{
				asset.second->wait();
			}
		}
	}

	void CAsset::init()
	{
		LOG_TRACE(U"CAsset::init()");

		LOG_INFO(U"ℹ️ Asset initialized");
	}

	void CAsset::update()
	{
		Siv3DEngine::Get<ISiv3DTexture>()->updateAsync(4);
	}

	bool CAsset::registerAsset(const AssetType assetType, const String& name, std::unique_ptr<IAsset>&& asset)
	{
		auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		if (assetList.find(name) != assetList.end())
		{
			LOG_FAIL(U"❌ {}Asset: Asset Name \"{}\" is already reserved. Use another name"_fmt(detail::GetAssetTypeName(assetType), name));

			return false;
		}

		auto result = assetList.emplace(name, std::move(asset));

		LOG_DEBUG(U"ℹ️ {}Asset: Asset \"{}\" registered"_fmt(detail::GetAssetTypeName(assetType), name));

		if (result.first.value()->getParameter().loadAsync)
		{
			result.first.value()->preloadAsync();

			return true;
		}
		else if (result.first.value()->getParameter().loadImmediately)
		{
			return result.first.value()->preload();
		}

		return true;
	}

	IAsset* CAsset::getAsset(const AssetType assetType, const String& name)
	{
		const auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		const auto it = assetList.find(name);

		if (it == assetList.end())
		{
			LOG_FAIL_ONCE(U"❌ CAsset::getAsset(): Unregistered {}Asset \"{}\""_fmt(detail::GetAssetTypeName(assetType), name));

			return nullptr;
		}

		IAsset* pAsset = it->second.get();

		if (!pAsset->isPreloaded())
		{
			if (pAsset->isLoadingAsync())
			{
				return nullptr;
			}

			if (!pAsset->preload())
			{
				return nullptr;
			}	
		}

		return pAsset;
	}

	bool CAsset::isRegistered(AssetType assetType, const String& name) const
	{
		const auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		return assetList.find(name) != assetList.end();
	}

	bool CAsset::preload(AssetType assetType, const String& name)
	{
		const auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		const auto it = assetList.find(name);

		if (it == assetList.end())
		{
			LOG_FAIL_ONCE(U"❌ CAsset::preload(): Unregistered {}Asset: \"{}\""_fmt(detail::GetAssetTypeName(assetType), name));

			return false;
		}

		IAsset* pAsset = it->second.get();

		if (!pAsset->isPreloaded())
		{
			pAsset->wait();

			pAsset->preload();

			LOG_DEBUG(U"ℹ️ {}Asset: \"{}\" preloaded"_fmt(detail::GetAssetTypeName(assetType), name));
		}

		return pAsset->loadSucceeded();
	}

	void CAsset::release(AssetType assetType, const String& name)
	{
		const auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		const auto it = assetList.find(name);

		if (it == assetList.end())
		{
			LOG_FAIL_ONCE(U"❌ CAsset::release(): Unregistered {}Asset: \"{}\""_fmt(detail::GetAssetTypeName(assetType), name));

			return;
		}

		IAsset* pAsset = it->second.get();

		pAsset->wait();

		pAsset->release();

		LOG_DEBUG(U"ℹ️ {}Asset: \"{}\" released"_fmt(detail::GetAssetTypeName(assetType), name));
	}

	void CAsset::releaseAll(const AssetType assetType)
	{
		auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		for (auto& asset : assetList)
		{
			asset.second->wait();

			asset.second->release();
		}
	}

	void CAsset::unregister(AssetType assetType, const String& name)
	{
		auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		const auto it = assetList.find(name);

		if (it == assetList.end())
		{
			LOG_FAIL_ONCE(U"❌ CAsset::unregister(): Unregistered {}Asset: \"{}\""_fmt(detail::GetAssetTypeName(assetType), name));

			return;
		}

		IAsset* pAsset = it->second.get();

		pAsset->wait();

		pAsset->release();

		assetList.erase(it);

		LOG_DEBUG(U"ℹ️ {}Asset: \"{}\" unregistered"_fmt(detail::GetAssetTypeName(assetType), name));
	}

	void CAsset::unregisterAll(const AssetType assetType)
	{
		auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		for (auto& asset : assetList)
		{
			asset.second->wait();

			asset.second->release();
		}

		assetList.clear();
	}

	bool CAsset::isReady(const AssetType assetType, const String& name) const
	{
		const auto& assetList = m_assetLists[static_cast<size_t>(assetType)];

		const auto it = assetList.find(name);

		if (it == assetList.end())
		{
			LOG_FAIL_ONCE(U"❌ CAsset::isReady(): Unregistered {}Asset: \"{}\""_fmt(detail::GetAssetTypeName(assetType), name));

			return false;
		}

		return it->second->isReady();
	}
}
