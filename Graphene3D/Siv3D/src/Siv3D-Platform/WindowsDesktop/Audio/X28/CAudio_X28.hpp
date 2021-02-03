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

# pragma once
# include <atomic>
# include <Siv3D/Windows.hpp>
# include <XAudio2.h>
# include <XAudio2fx.h>
# include <X3DAudio.h>
# include <Audio/IAudio.hpp>
# include <AssetHandleManager/AssetHandleManager.hpp>
# include "Audio_X28.hpp"

namespace s3d
{
	class CAudio_X28 : public ISiv3DAudio
	{
	private:

		HMODULE m_xaudio28 = nullptr;

		decltype(XAudio2Create)* p_XAudio2Create = nullptr;

		AudioDevice_X28 m_device;

		double m_masterVolume = 1.0;

		AssetHandleManager<AudioID, Audio_X28> m_audios{ U"Audio" };

		inline static std::atomic<bool> fadeControlEnabled = true;

		std::thread m_fadeControllerThread;

		std::mutex m_mutex;

	public:

		CAudio_X28();

		~CAudio_X28() override;

		bool hasAudioDevice() const override;

		bool init() override;

		AudioID create(Wave&& wave) override;

		void release(AudioID handleID) override;

		uint32 samplingRate(AudioID handleID) override;

		size_t samples(AudioID handleID) override;

		void setLoop(AudioID handleID, bool loop, int64 loopBeginSample, int64 loopEndSample) override;

		Optional<AudioLoopTiming> getLoop(AudioID handleID) override;

		bool play(AudioID handleID, const SecondsF& fadeinDuration) override;

		void pause(AudioID handleID, const SecondsF& fadeoutDuration) override;

		void stop(AudioID handleID, const SecondsF& fadeoutDuration) override;

		void playOneShot(AudioID handleID, double volume, double pitch) override;

		void stopAllShots(AudioID handleID) override;

		bool isPlaying(AudioID handleID) override;

		bool isPaused(AudioID handleID) override;

		uint64 posSample(AudioID handleID) override;

		uint64 streamPosSample(AudioID handleID) override;

		uint64 samplesPlayed(AudioID handleID) override;

		const Wave& getWave(AudioID handleID) override;

		void setPosSample(AudioID handleID, int64 sample) override;

		void setVolume(AudioID handleID, const std::pair<double, double>& volume) override;

		std::pair<double, double> getVolume(AudioID handleID) override;

		void setSpeed(AudioID handleID, double speed) override;

		double getSpeed(AudioID handleID) override;

		std::pair<double, double> getMinMaxSpeed(AudioID handleID) override;

		bool updateFade() override;
		
		void fadeMasterVolume() override;
	};
}
