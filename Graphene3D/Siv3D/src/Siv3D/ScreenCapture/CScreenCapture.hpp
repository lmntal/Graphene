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
# include "IScreenCapture.hpp"
# include <Siv3D/Array.hpp>
# include <Siv3D/String.hpp>

namespace s3d
{
	class CScreenCapture : public ISiv3DScreenCapture
	{
	private:

		FilePath m_defaultScreenshotDirectory;

		Array<FilePath> m_requestedPaths;

		bool m_hasRequest = false;

		bool m_hasNewFrame = false;

	public:

		CScreenCapture();

		~CScreenCapture() override;

		void init() override;

		void update() override;

		const FilePath& getDefaultScreenshotDirectory() const override;

		void requestScreenCapture(const FilePath& path) override;

		bool hasNewFrame() const override;

		const Image& receiveScreenCapture() const override;
	};
}
