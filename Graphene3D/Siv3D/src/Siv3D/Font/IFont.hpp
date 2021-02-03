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
# include <Siv3D/Font.hpp>

namespace s3d
{
	struct FontFace;

	class ISiv3DFont
	{
	public:

		static ISiv3DFont* Create();

		virtual ~ISiv3DFont() = default;

		virtual void init() = 0;

		virtual Image getColorEmoji(StringView emoji) = 0;

		virtual Image getColorEmojiSilhouette(StringView emoji) = 0;

		virtual Image getAwesomeIcon(uint16 code, int32 size) = 0;

		virtual Optional<const FontFace&> getAwesomeIconFontFaceFotCode(uint16 code) const = 0;

		virtual FontID create(Typeface typeface, int32 fontSize, FontStyle style) = 0;

		virtual FontID create(const FilePath& path, int32 fontSize, FontStyle style) = 0;

		virtual void release(FontID handleID) = 0;

		virtual const String& getFamilyName(FontID handleID) = 0;

		virtual const String& getStyleName(FontID handleID) = 0;

		virtual int32 getFontSize(FontID handleID) = 0;

		virtual int32 getAscent(FontID handleID) = 0;

		virtual int32 getDescent(FontID handleID) = 0;

		virtual Array<Glyph> getGlyphs(FontID handleID, const String& codePoints) = 0;

		virtual Array<Glyph> getVerticalGlyphs(FontID handleID, const String& codePoints) = 0;

		virtual OutlineGlyph getOutlineGlyph(FontID handleID, char32 codePoint) = 0;

		virtual const Texture& getTexture(FontID handleID) = 0;

		virtual RectF getBoundingRect(FontID handleID, const String& codePoints, double lineSpacingScale) = 0;

		virtual RectF getRegion(FontID handleID, const String& codePoints, double lineSpacingScale) = 0;

		virtual Array<int32> getXAdvances(FontID handleID, const String& codePoints) = 0;

		virtual RectF draw(FontID handleID, const String& codePoints, const Vec2& pos, const ColorF& color, double lineSpacingScale) = 0;

		virtual bool draw(FontID handleID, const String& codePoints, const RectF& area, const ColorF& color, double lineSpacingScale) = 0;

		virtual Rect paint(FontID handleID, Image& dst, const String& codePoints, const Point& pos, const Color& color, double lineSpacingScale) = 0;

		virtual Rect overwrite(FontID handleID, Image& dst, const String& codePoints, const Point& pos, const Color& color, double lineSpacingScale) = 0;
	};
}
