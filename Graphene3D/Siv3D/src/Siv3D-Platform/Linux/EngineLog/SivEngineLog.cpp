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

# include <mutex>
# include <iostream>
# include <Siv3D/EngineLog.hpp>
# include <Siv3D/Time.hpp>

namespace s3d
{
	namespace detail
	{
		int64 g_applicationTime = Time::GetMillisec();

		std::mutex g_logMutex;

		void OutputEnginelog(const StringView text)
		{
			const int64 timeStamp = Time::GetMillisec() - g_applicationTime;

			const std::string output = U"{}: [trace] {}\n"_fmt(timeStamp, text).narrow();

			std::lock_guard lock(g_logMutex);

			std::cout << output;
		}
	}
}
