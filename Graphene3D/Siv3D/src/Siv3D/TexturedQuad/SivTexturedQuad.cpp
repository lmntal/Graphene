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

# include <Siv3DEngine.hpp>
# include <Renderer2D/IRenderer2D.hpp>
# include <Siv3D/Texture.hpp>
# include <Siv3D/TexturedQuad.hpp>
# include <Siv3D/FloatQuad.hpp>

namespace s3d
{
	TexturedQuad::TexturedQuad()
	{

	}

	TexturedQuad::TexturedQuad(
		const Texture& _texture,
		float l,
		float t,
		float r,
		float b,
		const Quad& _quad,
		const Float2& _center)
		: quad(_quad)
		, texture(_texture)
		, uvRect(l, t, r, b)
		, center(_center)
	{
	
	}

	TexturedQuad::TexturedQuad(
		const Texture& _texture,
		const FloatRect& _uvRect,
		const Quad& _quad,
		const Float2& _center)
		: quad(_quad)
		, texture(_texture)
		, uvRect(_uvRect)
		, center(_center)
	{
	
	}

	const Quad& TexturedQuad::draw(const ColorF& diffuse) const
	{
		Siv3DEngine::Get<ISiv3DRenderer2D>()->addTexturedQuad(texture, quad, uvRect, diffuse.toFloat4());

		return quad;
	}

	Quad TexturedQuad::draw(const double x, const double y, const ColorF& diffuse) const
	{
		const Quad q = quad.movedBy(x, y);

		Siv3DEngine::Get<ISiv3DRenderer2D>()->addTexturedQuad(texture, q, uvRect, diffuse.toFloat4());

		return q;
	}

	Quad TexturedQuad::draw(const Vec2& pos, const ColorF& diffuse) const
	{
		return draw(pos.x, pos.y, diffuse);
	}

	Quad TexturedQuad::drawAt(const double x, const double y, const ColorF& diffuse) const
	{
		const Quad q = quad.movedBy(x - center.x, y - center.y);

		Siv3DEngine::Get<ISiv3DRenderer2D>()->addTexturedQuad(texture, q, uvRect, diffuse.toFloat4());

		return q;
	}

	Quad TexturedQuad::drawAt(const Vec2& pos, const ColorF& diffuse) const
	{
		return drawAt(pos.x, pos.y, diffuse);
	}
}
