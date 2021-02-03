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

# include <Siv3D/LineString.hpp>
# include <Siv3D/Optional.hpp>
# include <Siv3D/Rectangle.hpp>
# include <Siv3D/Spline.hpp>
# include <Siv3D/Polygon.hpp>
# include <Siv3DEngine.hpp>
# include <Renderer2D/IRenderer2D.hpp>

namespace s3d
{
	namespace detail
	{
		static constexpr bool Open = false;

		static constexpr bool Closed = true;
	}

	LineString::LineString(const LineString& lines)
		: base_type(lines.begin(), lines.end())
	{

	}

	LineString::LineString(LineString&& lines)
		: base_type(std::move(lines))
	{

	}

	LineString::LineString(const Array<Vec2>& points)
		: base_type(points.begin(), points.end())
	{

	}

	LineString::LineString(Array<Vec2>&& points)
		: base_type(std::move(points))
	{

	}

	LineString& LineString::operator =(const Array<Vec2>& other)
	{
		base_type::operator=(other);

		return *this;
	}

	LineString& LineString::operator =(Array<Vec2>&& other) noexcept
	{
		base_type::operator=(std::move(other));

		return *this;
	}

	LineString& LineString::operator =(const LineString& other)
	{
		base_type::operator=(other);

		return *this;
	}

	LineString& LineString::operator =(LineString&& other) noexcept
	{
		base_type::operator=(std::move(other));

		return *this;
	}

	void LineString::assign(const LineString& other)
	{
		base_type::operator=(other);
	}

	void LineString::assign(LineString&& other) noexcept
	{
		base_type::operator=(std::move(other));
	}

	LineString& LineString::operator <<(const Vec2& value)
	{
		base_type::push_back(value);

		return *this;
	}

	void LineString::swap(LineString& other) noexcept
	{
		base_type::swap(other);
	}

	LineString& LineString::append(const Array<Vec2>& other)
	{
		base_type::insert(end(), other.begin(), other.end());

		return *this;
	}

	LineString& LineString::append(const LineString& other)
	{
		base_type::insert(end(), other.begin(), other.end());

		return *this;
	}

	LineString& LineString::remove(const Vec2& value)
	{
		base_type::remove(value);

		return *this;
	}

	LineString& LineString::remove_at(const size_t index)
	{
		base_type::remove_at(index);

		return *this;
	}

	LineString& LineString::reverse()
	{
		base_type::reverse();

		return *this;
	}

	LineString& LineString::rotate(const std::ptrdiff_t count)
	{
		base_type::rotate(count);

		return *this;
	}

	LineString& LineString::shuffle()
	{
		base_type::shuffle();

		return *this;
	}

	LineString LineString::slice(const size_t index) const
	{
		return LineString(base_type::slice(index));
	}

	LineString LineString::slice(const size_t index, const size_t length) const
	{
		return LineString(base_type::slice(index, length));
	}

	size_t LineString::num_lines() const noexcept
	{
		return size() < 2 ? 0 : size() - 1;
	}

	Line LineString::line(const size_t index) const
	{
		return{ base_type::operator[](index), base_type::operator[](index + 1) };
	}

	LineString LineString::movedBy(const double x, const double y) const
	{
		return LineString(*this).moveBy(x, y);
	}

	LineString LineString::movedBy(const Vec2& v) const
	{
		return movedBy(v.x, v.y);
	}

	LineString& LineString::moveBy(const double x, const double y) noexcept
	{
		for (auto& point : *this)
		{
			point.moveBy(x, y);
		}

		return *this;
	}

	LineString& LineString::moveBy(const Vec2& v) noexcept
	{
		return moveBy(v.x, v.y);
	}

	RectF LineString::calculateBoundingRect() const noexcept
	{
		if (isEmpty())
		{
			return RectF(0);
		}

		const Vec2* it = data();
		const Vec2* itEnd = it + size();

		double left = it->x;
		double top = it->y;
		double right = left;
		double bottom = top;
		++it;
		
		while (it != itEnd)
		{
			if (it->x < left)
			{
				left = it->x;
			}
			else if (right < it->x)
			{
				right = it->x;
			}

			if (it->y < top)
			{
				top = it->y;
			}
			else if (bottom < it->y)
			{
				bottom = it->y;
			}

			++it;
		}

		return RectF(left, top, right - left, bottom - top);
	}

	LineString LineString::catmullRom(const int32 interpolation) const
	{
		return _catmullRom(interpolation, detail::Open);
	}

	LineString LineString::catmullRomClosed(const int32 interpolation) const
	{
		return _catmullRom(interpolation, detail::Closed);
	}

	Polygon LineString::calculateBuffer(const double distance, const int32 quality) const
	{
		return _calculateBuffer(distance, quality, detail::Open);
	}

	Polygon LineString::calculateBufferClosed(const double distance, const int32 quality) const
	{
		return _calculateBuffer(distance, quality, detail::Closed);
	}

	const LineString& LineString::paint(Image& dst, const Color& color) const
	{
		return _paint(dst, 1, color, detail::Open);
	}

	const LineString& LineString::paint(Image& dst, const int32 thickness, const Color& color) const
	{
		return _paint(dst, thickness, color, detail::Open);
	}

	const LineString& LineString::paintClosed(Image& dst, const Color& color) const
	{
		return _paint(dst, 1, color, detail::Closed);
	}

	const LineString& LineString::paintClosed(Image& dst, const int32 thickness, const Color& color) const
	{
		return _paint(dst, thickness, color, detail::Closed);
	}

	const LineString& LineString::overwrite(Image& dst, const Color& color, const bool antialiased) const
	{
		return _overwrite(dst, 1, color, antialiased, detail::Open);
	}

	const LineString& LineString::overwrite(Image& dst, const int32 thickness, const Color& color, const bool antialiased) const
	{
		return _overwrite(dst, thickness, color, antialiased, detail::Open);
	}

	const LineString& LineString::overwriteClosed(Image& dst, const Color& color, const bool antialiased) const
	{
		return _overwrite(dst, 1, color, antialiased, detail::Closed);
	}

	const LineString& LineString::overwriteClosed(Image& dst, const int32 thickness, const Color& color, const bool antialiased) const
	{
		return _overwrite(dst, thickness, color, antialiased, detail::Closed);
	}

	const LineString& LineString::draw(const ColorF& color) const
	{
		return _draw(LineStyle::SquareCap, 1.0, color, detail::Open);
	}

	const LineString& LineString::draw(const double thickness, const ColorF& color) const
	{
		return _draw(LineStyle::SquareCap, thickness, color, detail::Open);
	}

	const LineString& LineString::draw(const LineStyle& style, const double thickness, const ColorF& color) const
	{
		return _draw(style, thickness, color, detail::Open);
	}

	const LineString& LineString::drawClosed(const ColorF& color) const
	{
		return _draw(LineStyle::SquareCap, 1.0, color, detail::Closed);
	}

	const LineString& LineString::drawClosed(const double thickness, const ColorF& color) const
	{
		return _draw(LineStyle::SquareCap, thickness, color, detail::Closed);
	}

	const LineString& LineString::drawClosed(const LineStyle& style, const double thickness, const ColorF& color) const
	{
		return _draw(style, thickness, color, detail::Closed);
	}

	void LineString::drawCatmullRom(const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(LineStyle::SquareCap, 1.0, color, interpolation, detail::Open);
	}

	void LineString::drawCatmullRom(const double thickness, const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(LineStyle::SquareCap, thickness, color, interpolation, detail::Open);
	}

	void LineString::drawCatmullRom(const LineStyle& style, const double thickness, const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(style, thickness, color, interpolation, detail::Open);
	}

	void LineString::drawCatmullRomClosed(const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(LineStyle::SquareCap, 1.0, color, interpolation, detail::Closed);
	}

	void LineString::drawCatmullRomClosed(const double thickness, const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(LineStyle::SquareCap, thickness, color, interpolation, detail::Closed);
	}

	void LineString::drawCatmullRomClosed(const LineStyle& style, const double thickness, const ColorF& color, const int32 interpolation) const
	{
		_drawCatmullRom(style, thickness, color, interpolation, detail::Closed);
	}

	LineString LineString::_catmullRom(const int32 interpolation, const bool isClosed) const
	{
		if (size() < 2)
		{
			return *this;
		}

		// [Siv3D ToDo] 最適化

		Array<Vec2> points;
		{
			points.reserve(size() + 2 + isClosed);

			if (isClosed)
			{
				points.push_back((*this)[size() - 1]);
			}
			else
			{
				points.push_back((*this)[0]);
			}

			for (const auto& point : *this)
			{
				points.push_back(point);
			}

			if (isClosed)
			{
				points.push_back((*this)[0]);
				points.push_back((*this)[1]);
			}
			else
			{
				points.push_back((*this)[size() - 1]);
			}
		}

		LineString splinePoints;
		{
			splinePoints.reserve((points.size() - 3)*interpolation + 1);

			for (size_t i = 1; i < points.size() - 2; ++i)
			{
				const bool isLast = (i + 1) == points.size() - 2;

				for (int32 t = 0; t < (interpolation + isLast); ++t)
				{
					const Vec2 p = Spline::CatmullRom(points[i - 1], points[i], points[i + 1], points[i + 2], t / static_cast<double>(interpolation));

					splinePoints.push_back(p);
				}
			}
		}

		return splinePoints;
	}

	const LineString& LineString::_draw(const LineStyle& style, const double thickness, const ColorF& color, const bool isClosed) const
	{
		if (size() < 2)
		{
			return *this;
		}

		Siv3DEngine::Get<ISiv3DRenderer2D>()->addLineString(
			style,
			data(),
			static_cast<uint16>(size()),
			s3d::none,
			static_cast<float>(thickness),
			false,
			color.toFloat4(),
			isClosed
		);

		return *this;
	}

	void LineString::_drawCatmullRom(const LineStyle& style, const double thickness, const ColorF& color, const int32 interpolation, const bool isClosed) const
	{
		_catmullRom(interpolation, isClosed)._draw(style, thickness, color, isClosed);
	}

	void Formatter(FormatData& formatData, const LineString& value)
	{
		formatData.string.append(value.join(U", "_sv, U"("_sv, U")"_sv));
	}
}
