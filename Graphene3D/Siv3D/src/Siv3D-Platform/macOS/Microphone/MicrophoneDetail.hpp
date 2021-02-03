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

# pragma once
# include <RtAudio/RtAudio.h>
# include <Siv3D/Microphone.hpp>

namespace s3d
{
	class Microphone::MicrophoneDetail
	{
	private:

		RtAudio m_device;
		
		Array<WaveSampleS16> m_buffer;
		
		size_t m_writePos = 0;
		
		uint32 m_samplingRate = Wave::DefaultSamplingRate;
		
		bool m_initialized = false;
		
		bool m_isRecording = false;
		
		bool m_loop = true;

		static int InputCallback_S16_1ch(void*, void *inputBuffer, unsigned int nBufferFrames, double, RtAudioStreamStatus, void* data);
		
		static int InputCallback_S16_2ch(void*, void *inputBuffer, unsigned int nBufferFrames, double, RtAudioStreamStatus, void* data);
		
		static int InputCallback_F32_1ch(void*, void *inputBuffer, unsigned int nBufferFrames, double, RtAudioStreamStatus, void* data);
		
		static int InputCallback_F32_2ch(void*, void *inputBuffer, unsigned int nBufferFrames, double, RtAudioStreamStatus, void* data);

	public:

		MicrophoneDetail();

		~MicrophoneDetail();

		bool init(const Optional<size_t> deviceID, RecordingFormat format, size_t bufferLength, bool loop);

		bool isAvailable() const;

		void release();

		bool start();

		void stop();

		bool isRecording() const;

		uint32 samplingRate() const;

		const Array<WaveSampleS16>& getBuffer() const;

		size_t posSample() const;
		
		bool onRead_S16_1ch(const int16*, size_t samples);

		bool onRead_S16_2ch(const WaveSampleS16*, size_t samples);
		
		bool onRead_F32_1ch(const float*, size_t samples);
		
		bool onRead_F32_2ch(const WaveSample*, size_t samples);
	};
}
