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

# include <Siv3D/Script.hpp>
# include <Siv3D/CustomStopwatch.hpp>
# include <Siv3D/Logger.hpp>
# include "ScriptBind.hpp"

namespace s3d
{
	using namespace AngelScript;

	using BindType = CustomStopwatch;

	static void ConstructB(bool startImmediately, BindType* self)
	{
		new(self) BindType(startImmediately);
	}

	static void Destruct(BindType *thisPointer)
	{
		thisPointer->~BindType();
	}

	void RegisterCustomStopwatch(asIScriptEngine *engine)
	{
		const char TypeName[] = "CustomStopwatch";
		int32 r = 0;

		r = engine->RegisterObjectBehaviour(TypeName, asBEHAVE_CONSTRUCT, "void f(bool startImmediately = false)", asFUNCTION(ConstructB), asCALL_CDECL_OBJLAST); assert(r >= 0);
		r = engine->RegisterObjectBehaviour(TypeName, asBEHAVE_DESTRUCT, "void f()", asFUNCTION(Destruct), asCALL_CDECL_OBJLAST); assert(r >= 0);

		r = engine->RegisterObjectMethod(TypeName, "int32 ms() const", asMETHOD(BindType, ms), asCALL_THISCALL); assert(r >= 0);
	}
}
