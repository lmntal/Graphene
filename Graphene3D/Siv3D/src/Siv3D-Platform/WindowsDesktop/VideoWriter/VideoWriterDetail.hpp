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
# include <Siv3D/VideoWriter.hpp>
# include <Siv3D/PointVector.hpp>
# include <Codec/CCodec.hpp>

namespace s3d
{
	class VideoWriter::VideoWriterDetail
	{
	private:

		MF_Functions m_functions;

		std::pair<int32, int32> m_fps;

		Size m_frameSize = Size(0, 0);

		UINT32 m_bitRate = 0;

		GUID m_encodingFormat = {};

		GUID m_inputFormat = {};

		DWORD m_streamIndex = 0;

		ComPtr<IMFSinkWriter> m_sinkWriter;

		ComPtr<IMFMediaBuffer> m_buffer;

		bool m_initiated = false;

		LONGLONG m_rtStart = 0;

		UINT64 m_rtDuration = 0;

		HRESULT initializeSinkWriter(const FilePath& path);

		bool writeFrame(const Image& image, LONGLONG start, LONGLONG duration);

	public:

		VideoWriterDetail();

		~VideoWriterDetail();

		bool open(const FilePath& path, const Size& frameSize, double fps);

		void close();

		bool isOpen() const;

		bool write(const Image& image);

		Size size() const;
	};
}
