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
# include <Siv3D/Circular.hpp>
# include <Siv3D/Logger.hpp>
# include "ScriptBind.hpp"

namespace s3d
{
	using namespace AngelScript;

	using ShapeType = Circular;

	static void Construct(const Circular& c, ShapeType* self)
	{
		new(self) ShapeType(c);
	}

	static void ConstructDD(double r, double theta, ShapeType* self)
	{
		new(self) ShapeType(r, theta);
	}

	static void ConstructV(const Vec2& v, ShapeType* self)
	{
		new(self) ShapeType(v);
	}

	void RegisterCircular(asIScriptEngine* engine)
	{
		constexpr char TypeName[] = "Circular";

		int32 r = 0;
		r = engine->RegisterObjectProperty(TypeName, "double r", asOFFSET(ShapeType, r)); assert(r >= 0);
		r = engine->RegisterObjectProperty(TypeName, "double theta", asOFFSET(ShapeType, theta)); assert(r >= 0);

		r = engine->RegisterObjectBehaviour(TypeName, asBEHAVE_CONSTRUCT, "void f(const Circular &in)", asFUNCTION(Construct), asCALL_CDECL_OBJLAST); assert(r >= 0);
		r = engine->RegisterObjectBehaviour(TypeName, asBEHAVE_CONSTRUCT, "void f(double r, double theta)", asFUNCTION(ConstructDD), asCALL_CDECL_OBJLAST); assert(r >= 0);
		r = engine->RegisterObjectBehaviour(TypeName, asBEHAVE_CONSTRUCT, "void f(const Vec2& in)", asFUNCTION(ConstructV), asCALL_CDECL_OBJLAST); assert(r >= 0);

		r = engine->RegisterObjectMethod(TypeName, "Vec2 opNeg() const", asMETHODPR(Circular, operator-, () const, Circular), asCALL_THISCALL); assert(r >= 0);
		r = engine->RegisterObjectMethod(TypeName, "Vec2 opAdd(const Vec2 &in) const", asMETHODPR(Circular, operator+, (const Vec2&) const, Vec2), asCALL_THISCALL); assert(r >= 0);
		r = engine->RegisterObjectMethod(TypeName, "Vec2 opSub(const Vec2 &in) const", asMETHODPR(Circular, operator-, (const Vec2&) const, Vec2), asCALL_THISCALL); assert(r >= 0);

		r = engine->RegisterObjectMethod(TypeName, "OffsetCircular rotated(double) const", asMETHOD(ShapeType, rotated), asCALL_THISCALL); assert(r >= 0);
		r = engine->RegisterObjectMethod(TypeName, "OffsetCircular& rotate(double)", asMETHOD(ShapeType, rotate), asCALL_THISCALL); assert(r >= 0);

		//r = engine->RegisterObjectMethod(TypeName, "Float2 toFloat2() const", asMETHOD(Circular, toFloat2), asCALL_THISCALL); assert(r >= 0);
		r = engine->RegisterObjectMethod(TypeName, "Vec2 toVec2() const", asMETHOD(Circular, toVec2), asCALL_THISCALL); assert(r >= 0);

		r = engine->RegisterObjectMethod(TypeName, "Vec2 opImplConv() const", asMETHOD(Circular, toVec2), asCALL_THISCALL); assert(r >= 0);

		// Circular(Arg::r, Arg::theta)
		// Circular(Arg::theta, Arg::r)
	}
}
