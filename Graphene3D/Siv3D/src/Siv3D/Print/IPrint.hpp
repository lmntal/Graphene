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
# include <Siv3D/Fwd.hpp>

namespace s3d
{
	class ISiv3DPrint
	{
	public:

		static ISiv3DPrint* Create();

		virtual ~ISiv3DPrint() = default;

		virtual void init() = 0;

		virtual void add(const String& text) = 0;

		virtual void draw() = 0;

		virtual void clear() = 0;

		virtual void showUnhandledEditingText(const String& text) = 0;
	};
}
