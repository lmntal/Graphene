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
# include <Siv3D/Windows.hpp>
# include <XAudio2_7/XAudio2.h>
# include <Siv3D/Audio.hpp>
# include <Siv3D/Optional.hpp>

namespace s3d
{
	class SimpleVoice_X27
	{
	private:

		IXAudio2SourceVoice* m_sourceVoice = nullptr;

	public:

		SimpleVoice_X27() = default;

		SimpleVoice_X27(IXAudio2* const xAudio2, const Wave& wave, const double volume, const double pitch)
		{
			const WAVEFORMATEX waveFormat =
			{
				static_cast<WORD>(WAVE_FORMAT_IEEE_FLOAT),
				2,
				wave.samplingRate(),
				wave.samplingRate() * sizeof(WaveSample),
				static_cast<WORD>(sizeof(WaveSample)),
				static_cast<WORD>(32),
				static_cast<WORD>(0)
			};

			if (FAILED(xAudio2->CreateSourceVoice(
				&m_sourceVoice,
				&waveFormat,
				0,
				std::max<float>(1.0f, static_cast<float>(pitch)),
				nullptr,
				nullptr
			)))
			{
				return;
			}

			const XAUDIO2_BUFFER buffer
			{
				XAUDIO2_END_OF_STREAM,
				static_cast<UINT32>(wave.size_bytes()),
				static_cast<const uint8*>(static_cast<const void*>(wave.data())),
				0,
				0,
				0,
				0,
				XAUDIO2_NO_LOOP_REGION,
				nullptr
			};

			m_sourceVoice->SubmitSourceBuffer(&buffer);

			if (volume != 1.0)
			{
				m_sourceVoice->SetVolume(static_cast<float>(volume));
			}

			if (pitch != 1.0)
			{
				m_sourceVoice->SetFrequencyRatio(static_cast<float>(pitch));
			}

			m_sourceVoice->Start();
		}

		~SimpleVoice_X27()
		{
			m_sourceVoice->Stop();

			m_sourceVoice->FlushSourceBuffers();

			m_sourceVoice->DestroyVoice();

			m_sourceVoice = nullptr;
		}
	};

	class VoiceStream_X27 : public IXAudio2VoiceCallback
	{
	private:

		IXAudio2* m_xAudio2 = nullptr;

		const Wave* m_pWave = nullptr;

		Wave m_buffer;

		Optional<AudioLoopTiming> m_loop;

		IXAudio2SourceVoice* m_sourceVoice = nullptr;

		uint64 m_samplesSent = 0;

		uint64 m_samplesOffsetP = 0;

		uint64 m_samplesOffsetN = 0;

		uint64 m_loopCount = 0;

		int64 m_readPos = 0;

		int64 m_writePos = 0;

		std::pair<double, double> m_volume = { 1.0, 1.0 };

		double m_fadeVolume = 1.0;

		double m_speed = 1.0;

		double m_maxSpeed = 2.0;

		bool m_is3D = false;

		bool m_isActive = false;

		bool m_isPaused = false;

		bool m_isEnd = false;

		bool m_useStretcher = false;

		void WINAPI OnVoiceProcessingPassStart(UINT32 bytes_required) override
		{
			//LOG_TEST(L"OnVoiceProcessingPassStart: {} bytes required"_fmt(bytes_required));

			const uint64 samplesPlayed = getSamplesPlayed();
			const uint64 samples_bufferTarget = m_pWave->samplingRate() / 30;
			const uint64 samples_buffer = m_samplesSent - samplesPlayed;

			if (bytes_required == 0 && samples_buffer > samples_bufferTarget / 2)
			{
				return;
			}

			const uint64 samples_required = bytes_required / sizeof(WaveSample);
			uint64 samples_toWrite = samples_required;

			if (samples_buffer < samples_bufferTarget)
			{
				samples_toWrite = std::max(samples_toWrite, samples_bufferTarget - samples_buffer);
			}

			samples_toWrite = std::min<uint64>(samples_toWrite, m_buffer.size());

			if (!m_loop)
			{
				if (m_useStretcher)
				{
					// [Siv3D ToDo]
				}
				else
				{
					processBufferNoLoop(samples_toWrite);
				}
			}
			else
			{
				if (m_useStretcher)
				{
					// [Siv3D ToDo]
				}
				else
				{
					processBufferLoop(samples_toWrite);
				}
			}
		}

		void WINAPI OnVoiceProcessingPassEnd() override
		{
			//LOG_TEST(L"OnVoiceProcessingPassEnd");
		}

		void WINAPI OnStreamEnd() override
		{
			//LOG_TEST(L"OnStreamEnd");

			m_sourceVoice->Stop();

			m_isActive = false;
			m_isPaused = false;
			m_isEnd = true;
			m_samplesOffsetP = 0;
			m_samplesOffsetN = 0;
			m_loopCount = 0;
			m_readPos = 0;
			m_writePos = 0;
			m_samplesSent = 0;
			m_volume = { 1.0, 1.0 };
			m_fadeVolume = 1.0;
			m_speed = 1.0;
			m_maxSpeed = 2.0;
			m_useStretcher = false;
			updateVolume();
			updateSpeed();
		}

		void WINAPI OnBufferStart(void*) override
		{
			//LOG_TEST(L"OnBufferStart");
		}

		void WINAPI OnBufferEnd(void*) override
		{
			//LOG_TEST(L"OnBufferEnd");
		}

		void WINAPI OnLoopEnd(void*) override
		{
			//LOG_TEST(L"OnLoopEnd");
		}

		void WINAPI OnVoiceError(void *, HRESULT) override
		{
			//LOG_TEST(L"OnVoiceError");
		}

		void processBufferNoLoop(uint64 samples_toRead)
		{
			if (m_readPos + samples_toRead > m_pWave->size())
			{
				samples_toRead = m_pWave->size() - m_readPos;
			}

			while (samples_toRead)
			{
				const uint64 samples_read = samples_toRead;
				const bool reachedEoS = (m_readPos + samples_read) == m_pWave->size();

				const XAUDIO2_BUFFER buffer =
				{
					reachedEoS ? static_cast<UINT32>(XAUDIO2_END_OF_STREAM) : 0,
					static_cast<UINT32>(samples_read * sizeof(WaveSample)),
					static_cast<const uint8*>(static_cast<const void*>(m_pWave->data())) + m_readPos * sizeof(WaveSample),
					0,
					static_cast<UINT32>(samples_read),
					0,
					0,
					XAUDIO2_NO_LOOP_REGION,
					nullptr
				};

				m_sourceVoice->SubmitSourceBuffer(&buffer);
				m_samplesSent += samples_read;

				samples_toRead -= samples_read;
				m_readPos += samples_read;
			}
		}

		void processBufferLoop(uint64 samples_toRead)
		{
			while (samples_toRead)
			{
				const uint64 samples_readable = m_loop->endPos - m_readPos;
				const uint64 samples_read = std::min<uint64>(samples_toRead, samples_readable);

				if (samples_read)
				{
					const XAUDIO2_BUFFER buffer =
					{
						static_cast<UINT32>(XAUDIO2_END_OF_STREAM) * 0,
						static_cast<UINT32>(samples_read * sizeof(WaveSample)),
						static_cast<const uint8*>(static_cast<const void*>(m_pWave->data())) + m_readPos * sizeof(WaveSample),
						0,
						static_cast<UINT32>(samples_read),
						0,
						0,
						XAUDIO2_NO_LOOP_REGION,
						nullptr
					};

					m_sourceVoice->SubmitSourceBuffer(&buffer);
					m_samplesSent += samples_read;

					samples_toRead -= samples_read;
				}

				if ((m_readPos += samples_read) == m_loop->endPos)
				{
					m_samplesOffsetN = m_samplesSent;

					m_readPos = m_loop->beginPos;

					++m_loopCount;

					m_samplesOffsetP = 0;
				}
			}
		}

		void updateVolume()
		{
			const float channelVolumes[2]
			{
				static_cast<float>(m_volume.first * m_fadeVolume),
				static_cast<float>(m_volume.second * m_fadeVolume)
			};

			m_sourceVoice->SetChannelVolumes(2, channelVolumes);
		}

		void updateSpeed()
		{
			m_sourceVoice->SetFrequencyRatio(static_cast<float>(m_speed));
		}

	public:

		VoiceStream_X27() = default;

		explicit VoiceStream_X27(IXAudio2* const xAudio2, const Wave& wave, const Optional<AudioLoopTiming>& loop)
			: m_xAudio2(xAudio2)
			, m_pWave(&wave)
			, m_buffer(wave.samplingRate(), WaveSample::Zero())
			, m_loop(loop)
		{

		}

		bool create(const bool is3D, const double maxSpeed)
		{
			const WAVEFORMATEX waveFormat =
			{
				static_cast<WORD>(WAVE_FORMAT_IEEE_FLOAT),
				static_cast<WORD>(m_is3D ? 1 : 2),
				m_pWave->samplingRate(),
				m_pWave->samplingRate() * sizeof(WaveSample),
				static_cast<WORD>(sizeof(WaveSample)),
				static_cast<WORD>(32),
				static_cast<WORD>(0)
			};

			if (FAILED(m_xAudio2->CreateSourceVoice(
				&m_sourceVoice,
				&waveFormat,
				is3D ? XAUDIO2_VOICE_USEFILTER : 0,
				static_cast<float>(maxSpeed),
				this,
				nullptr
			)))
			{
				return false;
			}

			m_isActive = false;
			m_isEnd = false;
			m_isPaused = false;
			m_samplesOffsetP = 0;
			m_samplesOffsetN = 0;
			m_loopCount = 0;
			m_readPos = 0;
			m_writePos = 0;
			m_samplesSent = 0;
			m_volume = { 1.0, 1.0 };
			m_fadeVolume = 1.0;
			m_speed = 1.0;
			m_maxSpeed = 2.0;
			m_useStretcher = false;

			return true;
		}

		void destroy()
		{
			if (m_sourceVoice)
			{
				m_sourceVoice->Stop();
				m_sourceVoice->FlushSourceBuffers();
				m_sourceVoice->DestroyVoice();
				m_sourceVoice = nullptr;
			}
		}

		void setFadeVolume(const double volume)
		{
			m_fadeVolume = volume;

			updateVolume();
		}

		void setLoop(const bool loop, const int64 loopBeginSample, const int64 loopEndSample)
		{
			if (!loop)
			{
				m_loop.reset();
			}
			else
			{
				m_loop.emplace(loopBeginSample, loopEndSample);
			}

			stop();
		}

		const Optional<AudioLoopTiming>& getLoop() const
		{
			return m_loop;
		}

		bool play()
		{
			if (m_isActive && !m_isPaused)
			{
				return true;
			}

			m_isActive = true;
			m_isPaused = false;
			m_isEnd = false;

			if (FAILED(m_sourceVoice->Start()))
			{
				m_isActive = false;

				return false;
			}

			return true;
		}

		void pause()
		{
			if (!m_isActive)
			{
				return;
			}

			m_sourceVoice->Stop();

			m_isPaused = true;
		}

		void stop()
		{
			if (!m_isActive)
			{
				return;
			}

			destroy();

			create(m_is3D, m_maxSpeed);
		}

		bool isPlaying() const
		{
			return m_isActive && !m_isPaused;
		}

		bool isPaused() const
		{
			return m_isPaused;
		}

		uint64 calculatePosSample()
		{
			const uint64 samplesPlayed = getSamplesPlayed();

			const uint64 tp = samplesPlayed + m_samplesOffsetP + ((m_loop && m_loopCount) ? m_loop->beginPos : 0);

			if (tp < m_samplesOffsetN)
			{
				if (m_loop)
				{
					return m_loop->endPos - (m_samplesOffsetN - tp);
				}
				else
				{
					return m_pWave->size() - (m_samplesOffsetN - tp);
				}
			}
			else
			{
				return tp - m_samplesOffsetN;
			}
		}

		uint64 getSamplesPlayed()
		{
			XAUDIO2_VOICE_STATE state;

			m_sourceVoice->GetState(&state);

			return state.SamplesPlayed;
		}

		void setReadPos(const int64 posSample)
		{
			m_readPos = posSample;

			m_samplesOffsetP = posSample;

			m_loopCount = 0;
		}

		void setVolume(const std::pair<double, double>& volume)
		{
			m_volume = volume;

			updateVolume();
		}

		std::pair<double, double> getVolume() const
		{
			return m_volume;
		}

		void setSpeed(const double speed)
		{
			m_speed = Clamp<double>(speed, XAUDIO2_MIN_FREQ_RATIO, m_maxSpeed);

			updateSpeed();
		}

		double getSpeed() const
		{
			return m_speed;
		}

		std::pair<double, double> getMinMaxSpeed() const
		{
			return{ XAUDIO2_MIN_FREQ_RATIO , m_maxSpeed };
		}

		uint64 streamPosSample() const
		{
			return static_cast<uint64>(m_readPos);
		}

		bool readchedEnd() const
		{
			return m_isEnd;
		}
	};
}
