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

# include "CAudio_AL.hpp"
# include <Siv3D/String.hpp>
# include <Siv3D/MathConstants.hpp>
# include <Siv3D/EngineLog.hpp>

namespace s3d
{
	CAudio_AL::CAudio_AL()
	{

	}

	CAudio_AL::~CAudio_AL()
	{
		LOG_TRACE(U"CAudio_AL::~~CAudio_AL()");
		
		m_audios.destroy();
		
		if (m_context)
		{
			m_device = ::alcGetContextsDevice(m_context);
			
			::alcMakeContextCurrent(nullptr);

			::alcDestroyContext(m_context);
			
			::alcCloseDevice(m_device);
		}
	}

	bool CAudio_AL::hasAudioDevice() const
	{
		// [Siv3D ToDo]
		return true;
	}

	bool CAudio_AL::init()
	{
		LOG_TRACE(U"CAudio_AL::init()");
		
		m_device = ::alcOpenDevice(nullptr);
		
		if (!m_device)
		{
			return false;
		}
		
		m_context = ::alcCreateContext(m_device, nullptr);
		
		if (!m_context)
		{
			return false;
		}
		
		if (!::alcMakeContextCurrent(m_context))
		{
			return false;
		}
		
		::alListener3f(AL_POSITION, 0, 0, 1.0f);
		::alListener3f(AL_VELOCITY, 0, 0, 0);
		const ALfloat listenerOri[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f };
		::alListenerfv(AL_ORIENTATION, listenerOri);
		
		auto nullAudio = std::make_unique<Audio_AL>(
			Wave(SecondsF(0.5), Arg::generator = [](double t) {
				return 0.5 * std::sin(t * Math::TwoPi) * std::sin(t * Math::TwoPi * 220.0 * (t * 4.0 + 1.0)); }));
		
		if (!nullAudio->isInitialized())
		{
			return false;
		}
		
		m_audios.setNullData(std::move(nullAudio));

		LOG_INFO(U"ℹ️ CAudio initialized");

		return true;
	}

	AudioID CAudio_AL::create(Wave&& wave)
	{
		if (!wave)
		{
			return AudioID::NullAsset();
		}
		
		auto audio = std::make_unique<Audio_AL>(std::move(wave));
		
		if (!audio->isInitialized())
		{
			return AudioID::NullAsset();
		}
		
		return m_audios.add(std::move(audio));
	}

	void CAudio_AL::release(const AudioID handleID)
	{
		m_audios.erase(handleID);
	}

	uint32 CAudio_AL::samplingRate(const AudioID handleID)
	{
		return m_audios[handleID]->getWave().samplingRate();
	}

	size_t CAudio_AL::samples(const AudioID handleID)
	{
		return m_audios[handleID]->getWave().size();
	}

	void CAudio_AL::setLoop(const AudioID handleID, const bool loop, const int64 loopBeginSample, const int64 loopEndSample)
	{
		return m_audios[handleID]->getStream().setLoop(loop, loopBeginSample, loopEndSample);
	}
	
	Optional<AudioLoopTiming> CAudio_AL::getLoop(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().getLoop();
	}

	bool CAudio_AL::play(const AudioID handleID, const SecondsF& fadeinDuration)
	{
		// [Siv3D ToDo]
		return m_audios[handleID]->getStream().play();
	}
	
	void CAudio_AL::pause(const AudioID handleID, const SecondsF& fadeoutDuration)
	{
		// [Siv3D ToDo]
		m_audios[handleID]->getStream().pause();
	}
	
	void CAudio_AL::stop(const AudioID handleID, const SecondsF& fadeoutDuration)
	{
		// [Siv3D ToDo]
		m_audios[handleID]->getStream().stop();
	}
	
	void CAudio_AL::playOneShot(const AudioID handleID, const double volume, const double pitch)
	{
		m_audios[handleID]->playOneShot(volume, pitch);
	}
	
	void CAudio_AL::stopAllShots(const AudioID handleID)
	{
		m_audios[handleID]->stopAllShots();
	}

	bool CAudio_AL::isPlaying(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().isPlaying();
	}

	bool CAudio_AL::isPaused(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().isPaused();
	}

	uint64 CAudio_AL::posSample(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().getPosSample();
	}

	uint64 CAudio_AL::streamPosSample(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().streamPosSample();
	}

	uint64 CAudio_AL::samplesPlayed(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().samplesPlayed();
	}

	const Wave& CAudio_AL::getWave(AudioID handleID)
	{
		return m_audios[handleID]->getWave();
	}

	void CAudio_AL::setPosSample(const AudioID handleID, const int64 sample)
	{
		const auto& audio = m_audios[handleID];
		const bool isPlaying = audio->getStream().isPlaying();
		audio->getStream().pause();
		
		audio->getStream().setPosSmaple(sample);
		
		if (isPlaying)
		{
			audio->getStream().play();
		}
	}

	void CAudio_AL::setVolume(const AudioID handleID, const std::pair<double, double>& volume)
	{
		m_audios[handleID]->getStream().setVolume(volume);
	}

	std::pair<double, double> CAudio_AL::getVolume(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().getVolume();
	}

	void CAudio_AL::setSpeed(const AudioID handleID, const double speed)
	{
		m_audios[handleID]->getStream().setSpeed(speed);
	}

	double CAudio_AL::getSpeed(const AudioID handleID)
	{
		return m_audios[handleID]->getStream().getSpeed();
	}

	std::pair<double, double> CAudio_AL::getMinMaxSpeed(const AudioID)
	{
		// [Siv3D ToDo]
		return{ 1.0 / 1024.0 , 2.0 };
	}

	bool CAudio_AL::updateFade()
	{
		// [Siv3D ToDo]
		return true;
	}

	void CAudio_AL::fadeMasterVolume()
	{
		// [Siv3D ToDo]
	}
}
