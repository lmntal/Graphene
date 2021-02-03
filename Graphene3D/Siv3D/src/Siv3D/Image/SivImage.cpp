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

# include <Siv3D/Image.hpp>
# include <Siv3D/ImageRegion.hpp>
# include <Siv3D/ImageProcessing.hpp>
# include <Siv3D/BinaryWriter.hpp>
# include <Siv3D/MemoryWriter.hpp>
# include <Siv3D/ByteArray.hpp>
# include <Siv3D/Dialog.hpp>
# include <Siv3D/EngineLog.hpp>
# include <Siv3D/Emoji.hpp>
# include <Siv3D/Icon.hpp>
# include <Siv3D/Polygon.hpp>
# include <Siv3D/MultiPolygon.hpp>
# include <Siv3D/OpenCV_Bridge.hpp>
# include <Siv3DEngine.hpp>
# include <ImageFormat/IImageFormat.hpp>
# include <ObjectDetection/IObjectDetection.hpp>

# include <opencv2/imgproc.hpp>
# include <opencv2/photo.hpp>

namespace s3d
{
	namespace detail
	{
		[[nodiscard]] inline constexpr bool IsValidSize(const size_t width, const size_t height)
		{
			return (width <= Image::MaxWidth) && (height <= Image::MaxHeight);
		}

		[[nodiscard]] static constexpr int32 ConvertBorderType(const BorderType borderType)
		{
			switch (borderType)
			{
			case BorderType::Replicate:
				return cv::BORDER_REPLICATE;
			//case BorderType::Wrap:
			//	return cv::BORDER_WRAP;
			case BorderType::Reflect:
				return cv::BORDER_REFLECT;
			case BorderType::Reflect_101:
				return cv::BORDER_REFLECT101;
			default:
				return cv::BORDER_DEFAULT;
			}
		}

		static void MakeSepia(const double levr, const double levg, const double levb, Color& pixel)
		{
			const double y = (0.299 * pixel.r + 0.587 * pixel.g + 0.114 * pixel.b);
			const double r = levr + y;
			const double g = levg + y;
			const double b = levb + y;
			pixel.r = r >= 255.0 ? 255 : r <= 0.0 ? 0 : static_cast<uint8>(r);
			pixel.g = g >= 255.0 ? 255 : g <= 0.0 ? 0 : static_cast<uint8>(g);
			pixel.b = b >= 255.0 ? 255 : b <= 0.0 ? 0 : static_cast<uint8>(b);
		}

		static void SetupPosterizeTable(const int32 level, uint8 table[256])
		{
			const int32 levN = Clamp(level, 2, 256) - 1;

			for (size_t i = 0; i < 256; ++i)
			{
				table[i] = static_cast<uint8>(std::floor(i / 255.0 * levN + 0.5) / levN * 255);
			}
		}

		static void SetupGammmaTable(const double gamma, uint8 table[256])
		{
			const double gammaInv = 1.0 / gamma;

			for (size_t i = 0; i < 256; ++i)
			{
				table[i] = static_cast<uint8>(std::pow(i / 255.0, gammaInv) * 255.0);
			}
		}

		static Color GetAverage(const Image & src, const Rect & rect)
		{
			const int32 count = rect.area();

			if (!count)
			{
				return Color(0);
			}

			int32 sumR = 0, sumG = 0, sumB = 0, sumA = 0;

			const size_t imgWidth = src.width();
			const int32 height = rect.h;
			const int32 width = rect.w;

			const Color* pLine = &src[rect.y][rect.x];

			for (int32 y = 0; y < height; ++y)
			{
				const Color* pDst = pLine;

				for (int32 x = 0; x < width; ++x)
				{
					sumR += pDst->r;
					sumG += pDst->g;
					sumB += pDst->b;
					sumA += pDst->a;
					++pDst;
				}

				pLine += imgWidth;
			}

			return Color(sumR / count, sumG / count, sumB / count, sumA / count);
		}

		static void FillRect(Image & dst, const Rect & rect, const Color & color)
		{
			const size_t imgWidth = dst.width();
			const int32 height = rect.h;
			const int32 width = rect.w;

			Color* pLine = &dst[rect.y][rect.x];

			for (int32 y = 0; y < height; ++y)
			{
				Color* pDst = pLine;

				for (int32 x = 0; x < width; ++x)
				{
					(*pDst++) = color;
				}

				pLine += imgWidth;
			}
		}

		[[nodiscard]] static MultiPolygon ToPolygonsWithoutHoles(const cv::Mat_<uint8>& gray)
		{
			MultiPolygon polygons;
			std::vector<std::vector<cv::Point>> contours;

			try
			{
				cv::findContours(gray, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE, { 0, 0 });
			}
			catch (cv::Exception&)
			{
				return polygons;
			}

			for (const auto& contour : contours)
			{
				const size_t externalSize = contour.size();

				if (externalSize < 3)
				{
					continue;
				}

				Array<Vec2> external(externalSize);
				{
					Vec2* pDst = external.data();
					const Vec2* const pDstEnd = pDst + externalSize;
					const cv::Point* pSrc = contour.data() + (externalSize - 1);

					while (pDst != pDstEnd)
					{
						pDst->set(pSrc->x, pSrc->y);
						++pDst; --pSrc;
					}
				}

				for (auto& polygon : Polygon::Correct(external))
				{
					if (polygon)
					{
						polygons.push_back(std::move(polygon));
					}
				}
			}

			return polygons;
		}

		[[nodiscard]] static MultiPolygon ToPolygons(const cv::Mat_<uint8>& gray)
		{
			MultiPolygon polygons;
			std::vector<std::vector<cv::Point>> contours;
			std::vector<cv::Vec4i> hierarchy;

			try
			{
				cv::findContours(gray, contours, hierarchy, cv::RETR_CCOMP, cv::CHAIN_APPROX_SIMPLE, { 0, 0 });
			}
			catch (cv::Exception&)
			{
				return polygons;
			}

			for (size_t i = 0; i < contours.size(); i = hierarchy[i][0])
			{
				const auto& contour = contours[i];
				const size_t externalSize = contour.size();

				if (externalSize < 3)
				{
					continue;
				}

				Array<Vec2> external(externalSize);
				{
					{
						Vec2* pDst = external.data();
						const Vec2* const pDstEnd = pDst + externalSize;
						const cv::Point* pSrc = contour.data() + (externalSize - 1);

						while (pDst != pDstEnd)
						{
							pDst->set(pSrc->x, pSrc->y);
							++pDst; --pSrc;
						}
					}

					for (size_t k = 0; k < externalSize; ++k)
					{
						const Vec2& a = external[k];

						for (size_t m = k + 1; m < externalSize; ++m)
						{
							if (Vec2& b = external[m]; a == b)
							{
								b += ((external[m - 1] - b).normalized() * 0.5).rotated(90_deg);
							}
						}
					}

					//{
					//	Vec2* pDst = external.data();
					//	const Vec2* pDstEnd = external.data() + externalSize - 1;

					//	while (pDst != pDstEnd)
					//	{
					//		*pDst += ((*(pDst + 1) - *pDst).normalized() * 0.01);
					//		++pDst;
					//	}
					//}
				}

				Array<Array<Vec2>> holes;
				{
					for (int32 k = hierarchy[i][2]; k != -1; k = hierarchy[k][0])
					{
						const auto& holeContour = contours[k];
						const size_t holeSize = holeContour.size();

						Array<Vec2> hole(holeSize);
						{
							Vec2* pDst = hole.data();
							const Vec2* const pDstEnd = pDst + holeSize;
							const cv::Point* pSrc = holeContour.data() + (holeSize - 1);

							while (pDst != pDstEnd)
							{
								pDst->set(pSrc->x, pSrc->y);
								++pDst; --pSrc;
							}
						}

						holes.push_back(std::move(hole));
					}
				}

				for (auto& polygon : Polygon::Correct(external, holes))
				{
					if (polygon)
					{
						polygons.push_back(std::move(polygon));
					}
				}
			}

			return polygons;
		}

		[[nodiscard]] static Polygon SelectLargestPolygon(const MultiPolygon& polygons)
		{
			if (!polygons)
			{
				return Polygon();
			}
			else if (polygons.size() == 1)
			{
				return polygons.front();
			}

			double maxArea = 0.0;

			size_t index = 0;

			for (size_t i = 0; i < polygons.size(); ++i)
			{
				const double area = polygons[i].area();

				if (area > maxArea)
				{
					maxArea = area;

					index = i;
				}
			}

			return polygons[index];
		}
	}

	Image::Image(Image&& image) noexcept
		: m_data(std::move(image.m_data))
		, m_width(image.m_width)
		, m_height(image.m_height)
	{
		image.m_width = image.m_height = 0;
	}

	Image::Image(const size_t size)
		: Image(size, size)
	{
	
	}

	Image::Image(const size_t size, const Color& color)
		: Image(size, size, color)
	{
	
	}

	Image::Image(const Size& size)
		: Image(size.x, size.y)
	{
	
	}

	Image::Image(const Size& size, const Color& color)
		: Image(size.x, size.y, color)
	{
	
	}

	Image::Image(const Size& size, Arg::generator_<std::function<Color(void)>> generator)
		: Image(size.x, size.y, generator)
	{
	
	}

	Image::Image(const Size& size, Arg::generator_<std::function<Color(Point)>> generator)
		: Image(size.x, size.y, generator)
	{
	
	}

	Image::Image(const Size& size, Arg::generator_<std::function<Color(Vec2)>> generator)
		: Image(size.x, size.y, generator)
	{
	
	}

	Image::Image(const Size& size, Arg::generator0_1_<std::function<Color(Vec2)>> generator)
		: Image(size.x, size.y, generator)
	{
	
	}

	Image::Image(const size_t width, const size_t height)
		: m_data(detail::IsValidSize(width, height) ? width * height : 0)
		, m_width(detail::IsValidSize(width, height) ? static_cast<uint32>(width) : 0)
		, m_height(detail::IsValidSize(width, height) ? static_cast<uint32>(height) : 0)
	{

	}

	Image::Image(const size_t width, const size_t height, const Color& color)
		: m_data(detail::IsValidSize(width, height) ? width * height : 0, color)
		, m_width(detail::IsValidSize(width, height) ? static_cast<uint32>(width) : 0)
		, m_height(detail::IsValidSize(width, height) ? static_cast<uint32>(height) : 0)
	{

	}

	Image::Image(const size_t width, const size_t height, Arg::generator_<std::function<Color(void)>> generator)
		: Image(Generate(width, height, *generator))
	{
	
	}

	Image::Image(const size_t width, const size_t height, Arg::generator_<std::function<Color(Point)>> generator)
		: Image(Generate(width, height, *generator))
	{
	
	}

	Image::Image(const size_t width, const size_t height, Arg::generator_<std::function<Color(Vec2)>> generator)
		: Image(Generate(width, height, *generator))
	{
	
	}

	Image::Image(const size_t width, const size_t height, Arg::generator0_1_<std::function<Color(Vec2)>> generator)
		: Image(Generate0_1(width, height, *generator))
	{
	
	}

	Image::Image(const FilePath& path)
		: Image(Siv3DEngine::Get<ISiv3DImageFormat>()->load(path))
	{

	}

	Image::Image(IReader&& reader, const ImageFormat format)
		: Image(Siv3DEngine::Get<ISiv3DImageFormat>()->decode(std::move(reader), format))
	{

	}

	Image::Image(const FilePath& rgb, const FilePath& alpha)
		: Image(rgb)
	{
		applyAlphaFromRChannel(alpha);
	}

	Image::Image(const Color& rgb, const FilePath& alpha)
		: Image(alpha)
	{
		for (auto& pixel : *this)
		{
			pixel.a = pixel.r;
			pixel.r = rgb.r;
			pixel.g = rgb.g;
			pixel.b = rgb.b;
		}
	}

	Image::Image(const Emoji& emoji)
	{
		*this = Emoji::CreateImage(emoji.codePoints);
	}

	Image::Image(const Icon& icon)
	{
		*this = Icon::CreateImage(icon.code, icon.size);
	}

	Image::Image(const Grid<Color>& grid)
		: Image(grid.width(), grid.height())
	{
		if (m_data.empty())
		{
			return;
		}

		std::memcpy(m_data.data(), grid.data(), grid.size_bytes());
	}

	Image::Image(const Grid<ColorF>& grid)
		: Image(grid.width(), grid.height())
	{
		if (m_data.empty())
		{
			return;
		}

		const ColorF* pSrc = grid.data();
		const ColorF* const pSrcEnd = pSrc + grid.size_elements();
		Color* pDst = m_data.data();

		while (pSrc != pSrcEnd)
		{
			*pDst++ = *pSrc++;
		}
	}

	Image& Image::operator =(Image&& image) noexcept
	{
		m_data = std::move(image.m_data);
		m_width = image.m_width;
		m_height = image.m_height;

		image.m_width = image.m_height = 0;

		return *this;
	}

	Image& Image::assign(const Image& image)
	{
		return operator =(image);
	}

	Image& Image::assign(Image&& image) noexcept
	{
		return operator =(std::move(image));
	}

	void Image::release()
	{
		clear();

		shrink_to_fit();
	}

	void Image::swap(Image& image) noexcept
	{
		m_data.swap(image.m_data);

		std::swap(m_width, image.m_width);

		std::swap(m_height, image.m_height);
	}

	Image Image::cloned() const
	{
		return *this;
	}

	void Image::fill(const Color& color)
	{
		Color* pDst = m_data.data();
		Color* const pDstEnd = pDst + m_data.size();

		while (pDst != pDstEnd)
		{
			*pDst++ = color;
		}
	}

	void Image::resize(const size_t width, const size_t height)
	{
		if (!detail::IsValidSize(width, height))
		{
			return clear();
		}

		if (width == m_width && height == m_height)
		{
			return;
		}

		m_data.resize(width * height);

		m_width = static_cast<uint32>(width);

		m_height = static_cast<uint32>(height);
	}

	void Image::resize(const size_t width, const size_t height, const Color& fillColor)
	{
		if (!detail::IsValidSize(width, height))
		{
			return clear();
		}

		if (width == m_width && height == m_height)
		{
			return;
		}

		m_data.assign(width * height, fillColor);

		m_width = static_cast<uint32>(width);

		m_height = static_cast<uint32>(height);
	}

	void Image::resizeRows(const size_t rows, const Color& fillColor)
	{
		if (rows == m_height)
		{
			return;
		}

		if (!detail::IsValidSize(m_width, rows))
		{
			return clear();
		}

		if (rows < m_height)
		{
			m_data.resize(m_width * rows);
		}
		else
		{
			m_data.insert(m_data.end(), m_width * (rows - m_height), fillColor);
		}

		m_height = static_cast<uint32>(rows);
	}

	Image Image::clipped(const Rect& rect) const
	{
		if (!detail::IsValidSize(rect.w, rect.h))
		{
			return Image();
		}

		Image tmp(rect.size, Color(0, 0));

		const int32 h = static_cast<int32>(m_height);
		const int32 w = static_cast<int32>(m_width);

		// [Siv3D ToDo] 最適化
		for (int32 y = 0; y < rect.h; ++y)
		{
			const int32 sy = y + rect.y;

			if (0 <= sy && sy < h)
			{
				for (int32 x = 0; x < rect.w; ++x)
				{
					const int32 sx = x + rect.x;

					if (0 <= sx && sx < w)
					{
						tmp[y][x] = operator[](sy)[sx];
					}
				}
			}
		}

		return tmp;
	}

	Image Image::clipped(const int32 x, const int32 y, const int32 w, const int32 h) const
	{
		return clipped(Rect(x, y, w, h));
	}

	Image Image::clipped(const Point& pos, const int32 w, const int32 h) const
	{
		return clipped(Rect(pos, w, h));
	}

	Image Image::clipped(const int32 x, const int32 y, const Size& size) const
	{
		return clipped(Rect(x, y, size));
	}

	Image Image::clipped(const Point& pos, const Size& size) const
	{
		return clipped(Rect(pos, size));
	}

	Image Image::squareClipped() const
	{
		const int32 size = std::min(m_width, m_height);

		return clipped((m_width - size) / 2, (m_height - size) / 2, size, size);
	}

	Image& Image::forEach(std::function<void(Color&)> function)
	{
		for (auto& pixel : m_data)
		{
			function(pixel);
		}

		return *this;
	}

	Image& Image::swapRB()
	{
		for (auto& pixel : m_data)
		{
			const uint32 t = pixel.r;
			pixel.r = pixel.b;
			pixel.b = t;
		}

		return *this;
	}

	ColorF Image::sample_Repeat(const double x, const double y) const
	{
		const int32 ix = static_cast<int32>(x);
		const int32 iy = static_cast<int32>(y);

		const Color& c1 = getPixel_Repeat(ix, iy);
		const Color& c2 = getPixel_Repeat(ix + 1, iy);
		const Color& c3 = getPixel_Repeat(ix, iy + 1);
		const Color& c4 = getPixel_Repeat(ix + 1, iy + 1);

		const double xr1 = x - ix;
		const double yr1 = y - iy;

		const double r = Biliner(c1.r, c2.r, c3.r, c4.r, xr1, yr1);
		const double g = Biliner(c1.g, c2.g, c3.g, c4.g, xr1, yr1);
		const double b = Biliner(c1.b, c2.b, c3.b, c4.b, xr1, yr1);
		const double a = Biliner(c1.a, c2.a, c3.a, c4.a, xr1, yr1);

		return{ r / 255.0, g / 255.0, b / 255.0, a / 255.0 };
	}

	ColorF Image::sample_Clamp(const double x, const double y) const
	{
		const int32 ix = static_cast<int32>(x);
		const int32 iy = static_cast<int32>(y);

		const Color& c1 = getPixel_Clamp(ix, iy);
		const Color& c2 = getPixel_Clamp(ix + 1, iy);
		const Color& c3 = getPixel_Clamp(ix, iy + 1);
		const Color& c4 = getPixel_Clamp(ix + 1, iy + 1);

		const double xr1 = x - ix;
		const double yr1 = y - iy;

		const double r = Biliner(c1.r, c2.r, c3.r, c4.r, xr1, yr1);
		const double g = Biliner(c1.g, c2.g, c3.g, c4.g, xr1, yr1);
		const double b = Biliner(c1.b, c2.b, c3.b, c4.b, xr1, yr1);
		const double a = Biliner(c1.a, c2.a, c3.a, c4.a, xr1, yr1);

		return{ r / 255.0, g / 255.0, b / 255.0, a / 255.0 };
	}

	ColorF Image::sample_Mirror(const double x, const double y) const
	{
		const int32 ix = static_cast<int32>(x);
		const int32 iy = static_cast<int32>(y);

		const Color& c1 = getPixel_Mirror(ix, iy);
		const Color& c2 = getPixel_Mirror(ix + 1, iy);
		const Color& c3 = getPixel_Mirror(ix, iy + 1);
		const Color& c4 = getPixel_Mirror(ix + 1, iy + 1);

		const double xr1 = x - ix;
		const double yr1 = y - iy;

		const double r = Biliner(c1.r, c2.r, c3.r, c4.r, xr1, yr1);
		const double g = Biliner(c1.g, c2.g, c3.g, c4.g, xr1, yr1);
		const double b = Biliner(c1.b, c2.b, c3.b, c4.b, xr1, yr1);
		const double a = Biliner(c1.a, c2.a, c3.a, c4.a, xr1, yr1);

		return{ r / 255.0, g / 255.0, b / 255.0, a / 255.0 };
	}

	bool Image::applyAlphaFromRChannel(const FilePath& alpha)
	{
		if (isEmpty())
		{
			return false;
		}

		const Image alphaImage(alpha);

		if (alphaImage.isEmpty())
		{
			return false;
		}

		Color* pDst = data();
		const size_t dstStep = m_width;

		const Color* pSrc = alphaImage.data();
		const size_t srcStep = alphaImage.m_width;

		const uint32 w = std::min(m_width, alphaImage.m_width);
		const uint32 h = std::min(m_height, alphaImage.m_height);

		for (uint32 y = 0; y < h; ++y)
		{
			Color* pDstLine = pDst;
			const Color* pSrcLine = pSrc;

			for (uint32 x = 0; x < w; ++x)
			{
				(*pDstLine++).a = (*pSrcLine++).r;
			}

			pSrc += srcStep;
			pDst += dstStep;
		}

		return true;
	}

	bool Image::save(const FilePath& path, ImageFormat format) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::save(): Image is empty");
			return false;
		}

		if (format == ImageFormat::Unspecified)
		{
			format = Siv3DEngine::Get<ISiv3DImageFormat>()->getFormatFromFilePath(path);
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->save(*this, format, path);
	}

	bool Image::saveWithDialog() const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::saveWithDialog(): Image is empty");
			return false;
		}

		if (const auto path = Dialog::SaveImage())
		{
			return save(path.value());
		}
		else
		{
			return false;
		}
	}

	bool Image::savePNG(const FilePathView path, const PNGFilter::Flag filterFlag) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::savePNG(): Image is empty");
			return false;
		}

		BinaryWriter writer(path);

		if (!writer)
		{
			return false;
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->encodePNG(writer, *this, filterFlag);
	}

	bool Image::saveJPEG(const FilePathView path, const int32 quality) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::saveJPEG(): Image is empty");
			return false;
		}

		BinaryWriter writer(path);

		if (!writer)
		{
			return false;
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->encodeJPEG(writer, *this, quality);
	}

	bool Image::savePPM(const FilePathView path, const PPMType format) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::savePPM(): Image is empty");
			return false;
		}

		BinaryWriter writer(path);

		if (!writer)
		{
			return false;
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->encodePPM(writer, *this, format);
	}

	bool Image::saveWebP(const FilePathView path, const bool lossless, const double quality, const WebPMethod method) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::saveWebP(): Image is empty");
			return false;
		}

		BinaryWriter writer(path);

		if (!writer)
		{
			return false;
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->encodeWebP(writer, *this, lossless, quality, method);
	}

	ByteArray Image::encode(ImageFormat format) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::encode(): Image is empty");
			return ByteArray();
		}

		if (format == ImageFormat::Unspecified)
		{
			format = ImageFormat::PNG;
		}

		return Siv3DEngine::Get<ISiv3DImageFormat>()->encode(*this, format);
	}

	ByteArray Image::encodePNG(const PNGFilter::Flag filterFlag) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::encodePNG(): Image is empty");
			return ByteArray();
		}

		MemoryWriter writer;

		if (!Siv3DEngine::Get<ISiv3DImageFormat>()->encodePNG(writer, *this, filterFlag))
		{
			return ByteArray();
		}

		return writer.retrieve();
	}

	ByteArray Image::encodeJPEG(const int32 quality) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::encodeJPEG(): Image is empty");
			return ByteArray();
		}

		MemoryWriter writer;

		if (!Siv3DEngine::Get<ISiv3DImageFormat>()->encodeJPEG(writer, *this, quality))
		{
			return ByteArray();
		}

		return writer.retrieve();
	}

	ByteArray Image::encodePPM(const PPMType format) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::encodePPM(): Image is empty");
			return ByteArray();
		}

		MemoryWriter writer;

		if (!Siv3DEngine::Get<ISiv3DImageFormat>()->encodePPM(writer, *this, format))
		{
			return ByteArray();
		}

		return writer.retrieve();
	}

	ByteArray Image::encodeWebP(const bool lossless, const double quality, const WebPMethod method) const
	{
		if (isEmpty())
		{
			LOG_FAIL(U"Image::encodeWebP(): Image is empty");
			return ByteArray();
		}

		MemoryWriter writer;

		if (!Siv3DEngine::Get<ISiv3DImageFormat>()->encodeWebP(writer, *this, lossless, quality, method))
		{
			return ByteArray();
		}

		return writer.retrieve();
	}

	Image& Image::negate()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			for (auto& pixel : m_data)
			{
				pixel = ~pixel;
			}
		}

		return *this;
	}

	Image Image::negated() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		for (auto& pixel : image)
		{
			pixel = ~pixel;
		}

		return image;
	}

	Image& Image::grayscale()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			for (auto& pixel : m_data)
			{
				pixel.r = pixel.g = pixel.b = pixel.grayscale0_255();
			}
		}

		return *this;
	}

	Image Image::grayscaled() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		for (auto& pixel : image)
		{
			pixel.r = pixel.g = pixel.b = pixel.grayscale0_255();
		}

		return image;
	}

	Image& Image::sepia(const int32 level)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			const double levn = Clamp(level, 0, 255);
			const double levr = 0.956 * levn;
			const double levg = 0.274 * levn;
			const double levb = -1.108 * levn;

			for (auto& pixel : m_data)
			{
				detail::MakeSepia(levr, levg, levb, pixel);
			}
		}

		return *this;
	}

	Image Image::sepiaed(const int32 level) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		const double levn = Clamp(level, 0, 255);
		const double levr = 0.956 * levn;
		const double levg = 0.274 * levn;
		const double levb = -1.108 * levn;

		for (auto& pixel : image)
		{
			detail::MakeSepia(levr, levg, levb, pixel);
		}

		return image;
	}

	Image& Image::posterize(const int32 level)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			uint8 colorTable[256];

			detail::SetupPosterizeTable(level, colorTable);

			for (auto& pixel : m_data)
			{
				pixel.r = colorTable[pixel.r];
				pixel.g = colorTable[pixel.g];
				pixel.b = colorTable[pixel.b];
			}
		}

		return *this;
	}

	Image Image::posterized(const int32 level) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		uint8 colorTable[256];

		detail::SetupPosterizeTable(level, colorTable);

		for (auto& pixel : image)
		{
			pixel.r = colorTable[pixel.r];
			pixel.g = colorTable[pixel.g];
			pixel.b = colorTable[pixel.b];
		}

		return image;
	}

	Image& Image::brighten(const int32 level)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			if (level < 0)
			{
				for (auto& pixel : m_data)
				{
					pixel.r = std::max(static_cast<int32>(pixel.r) + level, 0);
					pixel.g = std::max(static_cast<int32>(pixel.g) + level, 0);
					pixel.b = std::max(static_cast<int32>(pixel.b) + level, 0);
				}
			}
			else if (level > 0)
			{
				for (auto& pixel : m_data)
				{
					pixel.r = std::min(static_cast<int32>(pixel.r) + level, 255);
					pixel.g = std::min(static_cast<int32>(pixel.g) + level, 255);
					pixel.b = std::min(static_cast<int32>(pixel.b) + level, 255);
				}
			}
		}

		return *this;
	}

	Image Image::brightened(const int32 level) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		if (level < 0)
		{
			for (auto& pixel : image)
			{
				pixel.r = std::max(static_cast<int32>(pixel.r) + level, 0);
				pixel.g = std::max(static_cast<int32>(pixel.g) + level, 0);
				pixel.b = std::max(static_cast<int32>(pixel.b) + level, 0);
			}
		}
		else if (level > 0)
		{
			for (auto& pixel : image)
			{
				pixel.r = std::min(static_cast<int32>(pixel.r) + level, 255);
				pixel.g = std::min(static_cast<int32>(pixel.g) + level, 255);
				pixel.b = std::min(static_cast<int32>(pixel.b) + level, 255);
			}
		}

		return image;
	}

	Image& Image::mirror()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			const int32 h = m_height, w = m_width, wHalf = m_width / 2;
			Color* line = m_data.data();

			for (int32 y = 0; y < h; ++y)
			{
				Color* lineA = line;
				Color* lineB = line + w - 1;;

				for (int32 x = 0; x < wHalf; ++x)
				{
					std::swap(*lineA, *lineB);
					++lineA;
					--lineB;
				}

				line += w;
			}
		}

		return *this;
	}

	Image Image::mirrored() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		const Color* pSrc = data();
		Color* pDst = image.data();
		const size_t width = m_width;

		for (uint32 y = 0; y < m_height; ++y)
		{
			for (uint32 x = 0; x < m_width; ++x)
			{
				*(pDst + width * y + x) = *(pSrc + width * y + width - x - 1);
			}
		}

		return image;
	}

	Image& Image::flip()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			const int32 h = m_height, s = stride();
			Array<Color> line(m_width);
			Color* lineU = m_data.data();
			Color* lineB = lineU + m_width * (h - 1);

			for (int32 y = 0; y < h / 2; ++y)
			{
				::memcpy(line.data(), lineU, s);
				::memcpy(lineU, lineB, s);
				::memcpy(lineB, line.data(), s);

				lineU += m_width;
				lineB -= m_width;
			}
		}

		return *this;
	}

	Image Image::flipped() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		const size_t _stride = stride();
		const Color* pSrc = data() + (m_height - 1) * m_width;
		Color * pDst = image.data();

		for (uint32 y = 0; y < m_height; ++y)
		{
			::memcpy(pDst, pSrc, _stride);
			pDst += m_width;
			pSrc -= m_width;
		}

		return image;
	}

	Image& Image::rotate90()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		// [Siv3D ToDo] 最適化
		{
			Image tmp(m_height, m_width);

			for (uint32 y = 0; y < m_height; ++y)
			{
				for (uint32 x = 0; x < m_width; ++x)
				{
					tmp[x][m_height - y - 1] = (*this)[y][x];
				}
			}

			swap(tmp);
		}

		return *this;
	}

	Image Image::rotated90() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(m_height, m_width);

		// [Siv3D ToDo] 最適化
		for (uint32 y = 0; y < m_height; ++y)
		{
			for (uint32 x = 0; x < m_width; ++x)
			{
				image[x][m_height - y - 1] = (*this)[y][x];
			}
		}

		return image;
	}

	Image& Image::rotate180()
	{
		std::reverse(begin(), end());

		return *this;
	}

	Image Image::rotated180() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		const Color* pSrc = data() + num_pixels() - 1;

		Color* pDst = image.data();

		const Color* pDstEnd = pDst + image.num_pixels();

		while (pDst != pDstEnd)
		{
			*pDst = *pSrc;

			++pDst;

			--pSrc;
		}

		return image;
	}

	Image& Image::rotate270()
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		// [Siv3D ToDo] 最適化
		{
			Image tmp(m_height, m_width);

			for (uint32 y = 0; y < m_height; ++y)
			{
				for (uint32 x = 0; x < m_width; ++x)
				{
					tmp[m_width - x - 1][y] = (*this)[y][x];
				}
			}

			swap(tmp);
		}

		return *this;
	}

	Image Image::rotated270() const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(m_height, m_width);

		for (uint32 y = 0; y < m_height; ++y)
		{
			for (uint32 x = 0; x < m_width; ++x)
			{
				image[m_width - x - 1][y] = (*this)[y][x];
			}
		}

		return image;
	}

	Image& Image::gammaCorrect(const double gamma)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			uint8 colorTable[256];

			detail::SetupGammmaTable(gamma, colorTable);

			for (auto& pixel : m_data)
			{
				pixel.r = colorTable[pixel.r];
				pixel.g = colorTable[pixel.g];
				pixel.b = colorTable[pixel.b];
			}
		}

		return *this;
	}

	Image Image::gammaCorrected(const double gamma) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		uint8 colorTable[256];

		detail::SetupGammmaTable(gamma, colorTable);

		for (auto& pixel : image)
		{
			pixel.r = colorTable[pixel.r];
			pixel.g = colorTable[pixel.g];
			pixel.b = colorTable[pixel.b];
		}

		return image;
	}

	Image& Image::threshold(const uint8 threshold, const bool inverse)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			const uint32 a = inverse ? 0 : 0x00FFffFF, b = inverse ? 0x00FFffFF : 0;

			Color* pDst = data();

			const Color* pDstEnd = pDst + num_pixels();

			const double thresholdF = threshold / 255.0;

			while (pDst != pDstEnd)
			{
				*static_cast<uint32*>(static_cast<void*>(pDst)) = (thresholdF < pDst->grayscale() ? a : b) | (pDst->a << 24);

				++pDst;
			}
		}

		return *this;
	}

	Image Image::thresholded(const uint8 threshold, const bool inverse) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		const unsigned a = inverse ? 0 : 0x00FFffFF, b = inverse ? 0x00FFffFF : 0;

		Color* pDst = image.data();

		const Color* pDstEnd = pDst + num_pixels();

		const double thresholdF = threshold / 255.0;

		while (pDst != pDstEnd)
		{
			*static_cast<uint32*>(static_cast<void*>(pDst)) = (thresholdF < pDst->grayscale() ? a : b) | (pDst->a << 24);

			++pDst;
		}

		return image;
	}

	Image & Image::adaptiveThreshold(const AdaptiveMethod method, int32 blockSize, const double c, const bool inverse)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (blockSize % 2 == 0)
			{
				++blockSize;
			}
		}

		// 2. 処理
		{
			static_assert((int32)AdaptiveMethod::Mean == cv::ADAPTIVE_THRESH_MEAN_C);
			static_assert((int32)AdaptiveMethod::Gaussian == cv::ADAPTIVE_THRESH_GAUSSIAN_C);

			cv::Mat_<uint8> gray(m_height, m_width);

			OpenCV_Bridge::ToGrayScale(*this, gray);

			cv::adaptiveThreshold(gray, gray, 255, static_cast<int32>(method), inverse ? cv::THRESH_BINARY_INV : cv::THRESH_BINARY, blockSize, c);

			OpenCV_Bridge::FromGrayScale(gray, *this, true);
		}

		return *this;
	}

	Image Image::adaptiveThresholded(const AdaptiveMethod method, int32 blockSize, const double c, const bool inverse) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (blockSize % 2 == 0)
			{
				++blockSize;
			}
		}

		static_assert((int32)AdaptiveMethod::Mean == cv::ADAPTIVE_THRESH_MEAN_C);
		static_assert((int32)AdaptiveMethod::Gaussian == cv::ADAPTIVE_THRESH_GAUSSIAN_C);

		cv::Mat_<uint8> gray(m_height, m_width);

		OpenCV_Bridge::ToGrayScale(*this, gray);

		cv::adaptiveThreshold(gray, gray, 255, static_cast<int32>(method), inverse ? cv::THRESH_BINARY_INV : cv::THRESH_BINARY, blockSize, c);

		Image image(*this);

		OpenCV_Bridge::FromGrayScale(gray, image, true);

		return image;
	}

	Image & Image::mosaic(const int32 size)
	{
		return mosaic(size, size);
	}

	Image& Image::mosaic(const int32 horizontal, const int32 vertical)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 1 || vertical < 1) || (horizontal == 1 && vertical == 1))
			{
				return *this;
			}
		}

		// 2. 処理
		{
			const uint32 xPiece = m_width / horizontal;
			const uint32 yPiece = m_height / vertical;
			uint32 yP = 0, xP = 0;

			for (yP = 0; yP < yPiece; ++yP)
			{
				for (xP = 0; xP < xPiece; ++xP)
				{
					const Rect rc(xP * horizontal, yP * vertical, horizontal, vertical);
					detail::FillRect(*this, rc, detail::GetAverage(*this, rc));
				}

				const Rect rc(xP * horizontal, yP * vertical, m_width - xP * horizontal, vertical);
				detail::FillRect(*this, rc, detail::GetAverage(*this, rc));
			}

			if (yP* vertical < m_height)
			{
				const int32 tY = m_height - yP * vertical;

				for (xP = 0; xP < xPiece; ++xP)
				{
					const Rect rc(xP * horizontal, yP * vertical, horizontal, tY);
					detail::FillRect(*this, rc, detail::GetAverage(*this, rc));
				}

				const Rect rc(xP * horizontal, yP * vertical, m_width - xP * horizontal, tY);
				detail::FillRect(*this, rc, detail::GetAverage(*this, rc));
			}
		}

		return *this;
	}

	Image Image::mosaiced(const int32 size) const
	{
		return mosaiced(size, size);
	}

	Image Image::mosaiced(const int32 horizontal, const int32 vertical) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 1 || vertical < 1) || (horizontal == 1 && vertical == 1))
			{
				return *this;
			}
		}

		Image image(*this);

		const uint32 xPiece = m_width / horizontal;
		const uint32 yPiece = m_height / vertical;
		uint32 yP = 0, xP = 0;

		for (yP = 0; yP < yPiece; ++yP)
		{
			for (xP = 0; xP < xPiece; ++xP)
			{
				const Rect rc(xP * horizontal, yP * vertical, horizontal, vertical);
				detail::FillRect(image, rc, detail::GetAverage(image, rc));
			}

			const Rect rc(xP * horizontal, yP * vertical, m_width - xP * horizontal, vertical);
			detail::FillRect(image, rc, detail::GetAverage(image, rc));
		}

		if (yP* vertical < m_height)
		{
			const int32 tY = m_height - yP * vertical;

			for (xP = 0; xP < xPiece; ++xP)
			{
				const Rect rc(xP * horizontal, yP * vertical, horizontal, tY);
				detail::FillRect(image, rc, detail::GetAverage(image, rc));
			}

			const Rect rc(xP * horizontal, yP * vertical, m_width - xP * horizontal, tY);
			detail::FillRect(image, rc, detail::GetAverage(image, rc));
		}

		return image;
	}

	Image& Image::spread(const int32 size)
	{
		return spread(size, size);
	}

	Image& Image::spread(const int32 horizontal, const int32 vertical)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		// 2. 処理
		// [Siv3D ToDo] 最適化
		{
			Image tmp(m_width, m_height);

			DefaultRNGType rng(12345);

			const int32 h2 = horizontal * 2;

			const int32 v2 = vertical * 2;

			for (int32 y = 0; y < static_cast<int32>(m_height); ++y)
			{
				for (int32 x = 0; x < static_cast<int32>(m_width); ++x)
				{
					const int32 xpos = x + int32(rng() % (h2 + 1)) - horizontal;

					const int32 ypos = y + int32(rng() % (v2 + 1)) - vertical;

					tmp[y][x] = getPixel_Mirror(xpos, ypos);
				}
			}

			swap(tmp);
		}

		return *this;
	}

	Image Image::spreaded(const int32 size) const
	{
		return spreaded(size, size);
	}

	Image Image::spreaded(const int32 horizontal, const int32 vertical) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		DefaultRNGType rng(12345);

		const int32 h2 = horizontal * 2;

		const int32 v2 = vertical * 2;

		// [Siv3D ToDo] 最適化
		for (int32 y = 0; y < static_cast<int32>(m_height); ++y)
		{
			for (int32 x = 0; x < static_cast<int32>(m_width); ++x)
			{
				const int32 xpos = x + int32(rng() % (h2 + 1)) - horizontal;

				const int32 ypos = y + int32(rng() % (v2 + 1)) - vertical;

				image[y][x] = getPixel_Mirror(xpos, ypos);
			}
		}

		return image;
	}

	Image & Image::blur(const int32 size)
	{
		return blur(size, size);
	}

	Image& Image::blur(const int32 horizontal, const int32 vertical)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		// 2. 処理
		{
			Image tmp(m_width, m_height);

			cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, static_cast<cv::Vec4b*>(static_cast<void*>(data())), stride());

			cv::Mat_<cv::Vec4b> matDst(tmp.height(), tmp.width(), static_cast<cv::Vec4b*>(static_cast<void*>(tmp.data())), tmp.stride());

			cv::blur(matSrc, matDst, cv::Size(horizontal * 2 + 1, vertical * 2 + 1));

			swap(tmp);
		}

		return *this;
	}

	Image Image::blurred(const int32 size) const
	{
		return blurred(size, size);
	}

	Image Image::blurred(const int32 horizontal, const int32 vertical) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, const_cast<cv::Vec4b*>(static_cast<const cv::Vec4b*>(static_cast<const void*>(data()))), stride());

		cv::Mat_<cv::Vec4b> matDst(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());

		cv::blur(matSrc, matDst, cv::Size(horizontal * 2 + 1, vertical * 2 + 1));

		return image;
	}

	Image& Image::medianBlur(int32 apertureSize)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (apertureSize < 1)
			{
				return *this;
			}

			if (apertureSize % 2 == 0)
			{
				++apertureSize;
			}
		}

		// 2. 処理
		{
			Image tmp(m_width, m_height);

			cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, static_cast<cv::Vec4b*>(static_cast<void*>(data())), stride());

			cv::Mat_<cv::Vec4b> matDst(tmp.height(), tmp.width(), static_cast<cv::Vec4b*>(static_cast<void*>(tmp.data())), tmp.stride());

			cv::medianBlur(matSrc, matDst, apertureSize);

			swap(tmp);
		}

		return *this;
	}

	Image Image::medianBlurred(int32 apertureSize) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (apertureSize < 1)
			{
				return *this;
			}

			if (apertureSize % 2 == 0)
			{
				++apertureSize;
			}
		}

		Image image(m_width, m_height);

		cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, const_cast<cv::Vec4b*>(static_cast<const cv::Vec4b*>(static_cast<const void*>(data()))), stride());

		cv::Mat_<cv::Vec4b> matDst(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());

		cv::medianBlur(matSrc, matDst, apertureSize);

		return image;
	}

	Image& Image::gaussianBlur(const int32 size, const BorderType borderType)
	{
		return gaussianBlur(size, size, borderType);
	}

	Image& Image::gaussianBlur(const int32 horizontal, const int32 vertical, const BorderType borderType)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		// 2. 処理
		{
			Image tmp(m_width, m_height);

			cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, static_cast<cv::Vec4b*>(static_cast<void*>(data())), stride());

			cv::Mat_<cv::Vec4b> matDst(tmp.height(), tmp.width(), static_cast<cv::Vec4b*>(static_cast<void*>(tmp.data())), tmp.stride());

			cv::GaussianBlur(matSrc, matDst, cv::Size(horizontal * 2 + 1, vertical * 2 + 1), 0.0, 0.0, detail::ConvertBorderType(borderType));

			swap(tmp);
		}

		return *this;
	}

	Image Image::gaussianBlurred(const int32 size, const BorderType borderType) const
	{
		return gaussianBlurred(size, size, borderType);
	}

	Image Image::gaussianBlurred(const int32 horizontal, const int32 vertical, const BorderType borderType) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if ((horizontal < 0 || vertical < 0) || (horizontal == 0 && vertical == 0))
			{
				return *this;
			}
		}

		Image image(m_width, m_height);

		cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, const_cast<cv::Vec4b*>(static_cast<const cv::Vec4b*>(static_cast<const void*>(data()))), stride());

		cv::Mat_<cv::Vec4b> matDst(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());

		cv::GaussianBlur(matSrc, matDst, cv::Size(horizontal * 2 + 1, vertical * 2 + 1), 0.0, 0.0, detail::ConvertBorderType(borderType));

		return image;
	}

	Image& Image::dilate(const int32 iterations)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			cv::Mat_<cv::Vec4b> mat(m_height, m_width, static_cast<cv::Vec4b*>(static_cast<void*>(data())), stride());
			cv::dilate(mat, mat, cv::Mat(), cv::Point(-1, -1), iterations);
		}

		return *this;
	}

	Image Image::dilated(const int32 iterations) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		cv::Mat_<cv::Vec4b> mat(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());
		cv::dilate(mat, mat, cv::Mat(), cv::Point(-1, -1), iterations);

		return image;
	}

	Image& Image::erode(const int32 iterations)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		// 2. 処理
		{
			cv::Mat_<cv::Vec4b> mat(m_height, m_width, static_cast<cv::Vec4b*>(static_cast<void*>(data())), stride());
			cv::erode(mat, mat, cv::Mat(), cv::Point(-1, -1), iterations);
		}

		return *this;
	}

	Image Image::eroded(const int32 iterations) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}
		}

		Image image(*this);

		cv::Mat_<cv::Vec4b> mat(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());
		cv::erode(mat, mat, cv::Mat(), cv::Point(-1, -1), iterations);

		return image;
	}

	Image& Image::floodFill(const Point & pos, const Color & color, const FloodFillConnectivity connectivity, const int32 lowerDifference, const int32 upperDifference)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (pos.x < 0 || static_cast<int32>(m_width) <= pos.x || pos.y < 0 || static_cast<int32>(m_height) <= pos.y)
			{
				return *this;
			}
		}

		// 2. 処理
		{
			cv::Mat_<cv::Vec3b> mat(m_height, m_width);
			{
				const Color* pSrc = m_data.data();

				for (uint32 y = 0; y < m_height; ++y)
				{
					auto* line = &mat(y, 0);

					for (uint32 x = 0; x < m_width; ++x)
					{
						line[x][0] = pSrc->b;
						line[x][1] = pSrc->g;
						line[x][2] = pSrc->r;

						++pSrc;
					}
				}
			}

			cv::floodFill(
				mat,
				{ pos.x, pos.y },
				cv::Scalar(color.b, color.g, color.r),
				nullptr,
				cv::Scalar::all(lowerDifference),
				cv::Scalar::all(upperDifference),
				static_cast<int32>(connectivity) | cv::FLOODFILL_FIXED_RANGE | (255 << 8)
			);

			{
				Color* pDst = m_data.data();

				for (uint32 y = 0; y < m_height; ++y)
				{
					const auto* line = &mat(y, 0);

					for (uint32 x = 0; x < m_width; ++x)
					{
						pDst->r = line[x][2];
						pDst->g = line[x][1];
						pDst->b = line[x][0];

						++pDst;
					}
				}
			}
		}

		return *this;
	}

	Image Image::floodFilled(const Point & pos, const Color & color, const FloodFillConnectivity connectivity, const int32 lowerDifference, const int32 upperDifference) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			if (pos.x < 0 || static_cast<int32>(m_width) <= pos.x || pos.y < 0 || static_cast<int32>(m_height) <= pos.y)
			{
				return *this;
			}
		}

		Image image(*this);

		{
			cv::Mat_<cv::Vec3b> mat(m_height, m_width);

			OpenCV_Bridge::ToMatVec3b(*this, mat);

			cv::floodFill(
				mat,
				{ pos.x, pos.y },
				cv::Scalar(color.b, color.g, color.r),
				nullptr,
				cv::Scalar::all(lowerDifference),
				cv::Scalar::all(upperDifference),
				static_cast<int32>(connectivity) | cv::FLOODFILL_FIXED_RANGE
			);

			OpenCV_Bridge::FromMat(mat, image, true);
		}

		return image;
	}

	Image& Image::scale(int32 width, int32 height, Interpolation interpolation)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			width = Clamp<int32>(width, 1, Image::MaxWidth);

			height = Clamp<int32>(height, 1, Image::MaxHeight);

			if (static_cast<int32>(m_width) == width && static_cast<int32>(m_height) == height)
			{
				return *this;
			}
		}

		const uint32 targetWidth = width, targetHeight = height;

		// 3. 処理
		{
			// TODO 再検討
			if (interpolation == Interpolation::Unspecified)
			{
				if (targetWidth >= m_width && targetHeight >= m_height)
				{
					interpolation = Interpolation::Lanczos;
				}
				else if (targetWidth < m_width / 2 || targetHeight < m_height / 2)
				{
					interpolation = Interpolation::Area;
				}
				else
				{
					interpolation = Interpolation::Lanczos;
				}
			}

			Image tmp(targetWidth, targetHeight);

			cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, const_cast<cv::Vec4b*>(static_cast<const cv::Vec4b*>(static_cast<const void*>(data()))), stride());
			cv::Mat_<cv::Vec4b> matDst(tmp.height(), tmp.width(), static_cast<cv::Vec4b*>(static_cast<void*>(tmp.data())), tmp.stride());

			cv::resize(matSrc, matDst, matDst.size(), 0, 0, static_cast<int32>(interpolation));

			swap(tmp);
		}

		return *this;
	}

	Image Image::scaled(int32 width, int32 height, Interpolation interpolation) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			width = Clamp<int32>(width, 1, Image::MaxWidth);

			height = Clamp<int32>(height, 1, Image::MaxHeight);

			if (static_cast<int32>(m_width) == width && static_cast<int32>(m_height) == height)
			{
				return *this;
			}
		}

		const uint32 targetWidth = width, targetHeight = height;

		// 3. 処理
		{
			// TODO 再検討
			if (interpolation == Interpolation::Unspecified)
			{
				if (targetWidth >= m_width && targetHeight >= m_height)
				{
					interpolation = Interpolation::Lanczos;
				}
				else if (targetWidth < m_width / 2 || targetHeight < m_height / 2)
				{
					interpolation = Interpolation::Area;
				}
				else
				{
					interpolation = Interpolation::Lanczos;
				}
			}

			Image image(targetWidth, targetHeight);

			cv::Mat_<cv::Vec4b> matSrc(m_height, m_width, const_cast<cv::Vec4b*>(static_cast<const cv::Vec4b*>(static_cast<const void*>(data()))), stride());
			cv::Mat_<cv::Vec4b> matDst(image.height(), image.width(), static_cast<cv::Vec4b*>(static_cast<void*>(image.data())), image.stride());

			cv::resize(matSrc, matDst, matDst.size(), 0, 0, static_cast<int32>(interpolation));

			return image;
		}
	}

	Image& Image::scale(const Size & size, const Interpolation interpolation)
	{
		return scale(size.x, size.y, interpolation);
	}

	Image Image::scaled(const Size & size, const Interpolation interpolation) const
	{
		return scaled(size.x, size.y, interpolation);
	}

	Image& Image::scale(const double scaling, const Interpolation interpolation)
	{
		return scale(static_cast<int32>(m_width * scaling), static_cast<int32>(m_height * scaling), interpolation);
	}

	Image Image::scaled(const double scaling, const Interpolation interpolation) const
	{
		return scaled(static_cast<int32>(m_width * scaling), static_cast<int32>(m_height * scaling), interpolation);
	}

	Image& Image::fit(int32 width, int32 height, const bool scaleUp, const Interpolation interpolation)
	{
		if (!scaleUp)
		{
			width = std::min(width, static_cast<int32>(m_width));
			height = std::min(height, static_cast<int32>(m_height));
		}

		const int32 w = m_width;
		const int32 h = m_height;
		double ws = static_cast<double>(width) / w;	// 何% scalingするか
		double hs = static_cast<double>(height) / h;

		int32 targetWidth, targetHeight;

		if (ws < hs)
		{
			targetWidth = width;
			targetHeight = std::max(static_cast<int32>(h * ws), 1);
		}
		else
		{
			targetWidth = std::max(static_cast<int32>(w * hs), 1);
			targetHeight = height;
		}

		return scale(targetWidth, targetHeight, interpolation);
	}

	Image Image::fitted(int32 width, int32 height, const bool scaleUp, const Interpolation interpolation) const
	{
		if (!scaleUp)
		{
			width = std::min(width, static_cast<int32>(m_width));
			height = std::min(height, static_cast<int32>(m_height));
		}

		const int32 w = m_width;
		const int32 h = m_height;
		double ws = static_cast<double>(width) / w;	// 何% scalingするか
		double hs = static_cast<double>(height) / h;

		int32 targetWidth, targetHeight;

		if (ws < hs)
		{
			targetWidth = width;
			targetHeight = std::max(static_cast<int32>(h * ws), 1);
		}
		else
		{
			targetWidth = std::max(static_cast<int32>(w * hs), 1);
			targetHeight = height;
		}

		return scaled(targetWidth, targetHeight, interpolation);
	}

	Image& Image::fit(const Size & size, const bool scaleUp, const Interpolation interpolation)
	{
		return fit(size.x, size.y, scaleUp, interpolation);
	}

	Image Image::fitted(const Size & size, const bool scaleUp, const Interpolation interpolation) const
	{
		return fitted(size.x, size.y, scaleUp, interpolation);
	}

	Image& Image::border(const int32 thickness, const Color& color)
	{
		return border(thickness, thickness, thickness, thickness, color);
	}

	Image Image::bordered(const int32 thickness, const Color& color) const
	{
		return bordered(thickness, thickness, thickness, thickness, color);
	}

	Image& Image::border(int32 top, int32 right, int32 bottom, int32 left, const Color& color)
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			top = std::max(0, top);
			right = std::max(0, right);
			bottom = std::max(0, bottom);
			left = std::max(0, left);

			if (top == 0 && right == 0 && bottom == 0 && left == 0)
			{
				return *this;
			}
		}

		Image tmp(left + m_width + right, top + m_height + bottom, color);

		const Color* pSrc = data();
		const size_t srcStride = stride();
		Color* pDst = tmp.data() + tmp.width() * top + left;
		const size_t srcStep = m_width;
		const size_t dstStep = tmp.width();

		for (uint32 y = 0; y < m_height; ++y)
		{
			std::memcpy(pDst, pSrc, srcStride);
			pSrc += srcStep;
			pDst += dstStep;
		}

		swap(tmp);

		return *this;
	}

	Image Image::bordered(int32 top, int32 right, int32 bottom, int32 left, const Color& color) const
	{
		// 1. パラメータチェック
		{
			if (isEmpty())
			{
				return *this;
			}

			top = std::max(0, top);
			right = std::max(0, right);
			bottom = std::max(0, bottom);
			left = std::max(0, left);

			if (top == 0 && right == 0 && bottom == 0 && left == 0)
			{
				return *this;
			}
		}

		Image image(left + m_width + right, top + m_height + bottom, color);

		const Color* pSrc = data();
		const size_t srcStride = stride();
		Color* pDst = image.data() + image.width() * top + left;
		const size_t srcStep = m_width;
		const size_t dstStep = image.width();

		for (uint32 y = 0; y < m_height; ++y)
		{
			std::memcpy(pDst, pSrc, srcStride);
			pSrc += srcStep;
			pDst += dstStep;
		}

		return image;
	}

	ImageRegion Image::operator ()(int32 x, int32 y, int32 w, int32 h) const
	{
		return operator()(Rect(x, y, w, h));
	}

	ImageRegion Image::operator ()(const Point& pos, int32 w, int32 h) const
	{
		return operator()(Rect(pos, w, h));
	}

	ImageRegion Image::operator ()(int32 x, int32 y, const Size& size) const
	{
		return operator()(Rect(x, y, size));
	}

	ImageRegion Image::operator ()(const Point& pos, const Size& size) const
	{
		return operator()(Rect(pos, size));
	}

	ImageRegion Image::operator ()(const Rect& rect) const
	{
		return ImageRegion(*this, rect);
	}

	Polygon Image::alphaToPolygon(const uint32 threshold, const bool allowHoles) const
	{
		return detail::SelectLargestPolygon(alphaToPolygons(threshold, allowHoles));
	}

	Polygon Image::alphaToPolygonCentered(const uint32 threshold, const bool allowHoles) const
	{
		return alphaToPolygon(threshold, allowHoles).movedBy(-size() * 0.5);
	}

	MultiPolygon Image::alphaToPolygons(const uint32 threshold, const bool allowHoles) const
	{
		if (isEmpty())
		{
			MultiPolygon();
		}

		cv::Mat_<uint8> gray(height() * 2, width() * 2);

		OpenCV_Bridge::AlphaToBinary2x(*this, gray, threshold);

		return allowHoles ? detail::ToPolygons(gray).scale(0.5) : detail::ToPolygonsWithoutHoles(gray).scale(0.5);
	}

	MultiPolygon Image::alphaToPolygonsCentered(const uint32 threshold, const bool allowHoles) const
	{
		return alphaToPolygons(threshold, allowHoles).movedBy(-size() * 0.5);
	}

	Polygon Image::grayscaleToPolygon(const uint32 threshold, const bool allowHoles) const
	{
		return detail::SelectLargestPolygon(grayscaleToPolygons(threshold, allowHoles));
	}

	Polygon Image::grayscaleToPolygonCentered(const uint32 threshold, const bool allowHoles) const
	{
		return grayscaleToPolygon(threshold, allowHoles).movedBy(-size() * 0.5);
	}

	MultiPolygon Image::grayscaleToPolygons(const uint32 threshold, const bool allowHoles) const
	{
		if (isEmpty())
		{
			MultiPolygon();
		}

		cv::Mat_<uint8> gray(height() * 2, width() * 2);

		OpenCV_Bridge::RedToBinary2x(*this, gray, threshold);

		return allowHoles ? detail::ToPolygons(gray).scale(0.5) : detail::ToPolygonsWithoutHoles(gray).scale(0.5);
	}

	MultiPolygon Image::grayscaleToPolygonsCentered(const uint32 threshold, const bool allowHoles) const
	{
		return grayscaleToPolygons(threshold, allowHoles).movedBy(-size() * 0.5);
	}

	Array<Rect> Image::detectObjects(const HaarCascade cascade, const int32 minNeighbors, const Size& minSize, const Optional<Size>& maxSize) const
	{
		return Siv3DEngine::Get<ISiv3DObjectDetection>()->detect(*this, cascade, minNeighbors, minSize, maxSize.value_or(Size(0, 0)));
	}

	Array<Rect> Image::detectObjects(const HaarCascade cascade, const Array<Rect>& regions, const int32 minNeighbors, const Size& minSize, const Optional<Size>& maxSize) const
	{
		return Siv3DEngine::Get<ISiv3DObjectDetection>()->detect(*this, cascade, regions, minNeighbors, minSize, maxSize.value_or(Size(0, 0)));
	}

	namespace ImageProcessing
	{
		void Sobel(const Image& src, Image& dst, const int32 dx, const int32 dy, int32 apertureSize)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return;
				}

				if (&src == &dst)
				{
					return;
				}

				if (apertureSize % 2 == 0)
				{
					++apertureSize;
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<uint8> gray(src.height(), src.width());

				OpenCV_Bridge::ToGrayScale(src, gray);

				cv::Mat_<uint8> detected_edges;

				cv::Sobel(gray, detected_edges, CV_8U, dx, dy, apertureSize);

				OpenCV_Bridge::FromGrayScale(detected_edges, dst, true);
			}
		}

		void Laplacian(const Image& src, Image& dst, int32 apertureSize)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return;
				}

				if (&src == &dst)
				{
					return;
				}

				if (apertureSize % 2 == 0)
				{
					++apertureSize;
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<uint8> gray(src.height(), src.width());

				OpenCV_Bridge::ToGrayScale(src, gray);

				cv::Mat_<uint8> detected_edges;

				cv::Laplacian(gray, detected_edges, CV_8U, apertureSize);

				OpenCV_Bridge::FromGrayScale(detected_edges, dst, true);
			}
		}

		void Canny(const Image& src, Image& dst, const uint8 lowThreshold, const uint8 highThreshold, int32 apertureSize, const bool useL2Gradient)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return;
				}

				if (&src == &dst)
				{
					return;
				}

				if (apertureSize % 2 == 0)
				{
					++apertureSize;
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<uint8> gray(src.height(), src.width());

				OpenCV_Bridge::ToGrayScale(src, gray);

				cv::Mat_<uint8> detected_edges;

				cv::blur(gray, detected_edges, cv::Size(3, 3));

				cv::Canny(detected_edges, detected_edges, lowThreshold, highThreshold, apertureSize, useL2Gradient);

				OpenCV_Bridge::FromGrayScale(detected_edges, dst, true);
			}
		}

		void EdgePreservingFilter(const Image& src, Image& dst, EdgePreservingFilterType filterType, double sigma_s, double sigma_r)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return dst.clear();
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<cv::Vec3b> matSrc(src.height(), src.width());

				OpenCV_Bridge::ToMatVec3b(src, matSrc);

				cv::Mat_<cv::Vec3b> matDst(src.height(), src.width());

				cv::edgePreservingFilter(matSrc, matDst,
					filterType == EdgePreservingFilterType::Recursive
					? cv::RECURS_FILTER : cv::NORMCONV_FILTER,
					static_cast<float>(sigma_s), static_cast<float>(sigma_r));

				OpenCV_Bridge::FromMat(matDst, dst, true);
			}
		}

		void DetailEnhance(const Image& src, Image& dst, double sigma_s, double sigma_r)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return dst.clear();
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<cv::Vec3b> matSrc(src.height(), src.width());

				OpenCV_Bridge::ToMatVec3b(src, matSrc);

				cv::Mat_<cv::Vec3b> matDst(src.height(), src.width());

				cv::detailEnhance(matSrc, matDst, static_cast<float>(sigma_s), static_cast<float>(sigma_r));

				OpenCV_Bridge::FromMat(matDst, dst, true);
			}
		}

		void Stylization(const Image& src, Image& dst, double sigma_s, double sigma_r)
		{
			// 1. パラメータチェック
			{
				if (!src)
				{
					return dst.clear();
				}
			}

			// 2. 出力画像のサイズ変更
			{
				dst.resize(src.size());

				::memcpy(dst.data(), src.data(), dst.size_bytes());
			}

			// 3. 処理
			{
				cv::Mat_<cv::Vec3b> matSrc(src.height(), src.width());

				OpenCV_Bridge::ToMatVec3b(src, matSrc);

				cv::Mat_<cv::Vec3b> matDst(src.height(), src.width());

				cv::stylization(matSrc, matDst, static_cast<float>(sigma_s), static_cast<float>(sigma_r));

				OpenCV_Bridge::FromMat(matDst, dst, true);
			}
		}

		ColorF SSIM(const Image& image1, const Image& image2)
		{
			if (image1.size() != image2.size())
			{
				return ColorF(1.0);
			}

			const double C1 = 6.5025, C2 = 58.5225;
			const int32 x = image1.width(), y = image1.height();

			cv::Mat_<cv::Vec3f> I1(y, x), I2(y, x);
			OpenCV_Bridge::ToMatVec3f255(image1, I1);
			OpenCV_Bridge::ToMatVec3f255(image2, I2);

			cv::Mat I2_2 = I2.mul(I2);        // I2^2
			cv::Mat I1_2 = I1.mul(I1);        // I1^2
			cv::Mat I1_I2 = I1.mul(I2);        // I1 * I2

			/*************************** END INITS **********************************/

			cv::Mat mu1, mu2;   // PRELIMINARY COMPUTING
			cv::GaussianBlur(I1, mu1, cv::Size(11, 11), 1.5);
			cv::GaussianBlur(I2, mu2, cv::Size(11, 11), 1.5);


			cv::Mat mu1_2 = mu1.mul(mu1);
			cv::Mat mu2_2 = mu2.mul(mu2);
			cv::Mat mu1_mu2 = mu1.mul(mu2);

			cv::Mat sigma1_2, sigma2_2, sigma12;

			cv::GaussianBlur(I1_2, sigma1_2, cv::Size(11, 11), 1.5);
			sigma1_2 -= mu1_2;

			cv::GaussianBlur(I2_2, sigma2_2, cv::Size(11, 11), 1.5);
			sigma2_2 -= mu2_2;

			cv::GaussianBlur(I1_I2, sigma12, cv::Size(11, 11), 1.5);
			sigma12 -= mu1_mu2;

			///////////////////////////////// FORMULA ////////////////////////////////
			cv::Mat t1, t2, t3;

			t1 = 2 * mu1_mu2 + C1;
			t2 = 2 * sigma12 + C2;
			t3 = t1.mul(t2);              // t3 = ((2*mu1_mu2 + C1).*(2*sigma12 + C2))

			t1 = mu1_2 + mu2_2 + C1;
			t2 = sigma1_2 + sigma2_2 + C2;
			t1 = t1.mul(t2);               // t1 =((mu1_2 + mu2_2 + C1).*(sigma1_2 + sigma2_2 + C2))

			cv::Mat ssim_map;
			cv::divide(t3, t1, ssim_map);      // ssim_map =  t3./t1;

			cv::Scalar mssim = cv::mean(ssim_map); // mssim = average of ssim map
			return ColorF(mssim[2], mssim[1], mssim[0], 1.0);
		}

		void Inpaint(const Image& image, const Image& maskImage, Image& result, int32 radius)
		{
			// 1. パラメータチェック
			{
				if (!image || !maskImage)
				{
					return;
				}

				if (image.size() != maskImage.size())
				{
					return;
				}

				radius = std::max(radius, 0);
			}

			// 2. 処理
			{
				cv::Mat_<cv::Vec3b> matSrc(image.height(), image.width());

				OpenCV_Bridge::ToMatVec3b(image, matSrc);

				cv::Mat_<uint8> matMask(image.height(), image.width());

				OpenCV_Bridge::RedToBinary(maskImage, matMask, 254);

				cv::Mat_<cv::Vec3b> matDst;

				cv::inpaint(matSrc, matMask, matDst, radius, cv::INPAINT_TELEA);

				OpenCV_Bridge::FromMat(matDst, result, true);
			}
		}

		void Inpaint(const Image& image, const Grid<uint8>& maskImage, Image& result, int32 radius)
		{
			// 1. パラメータチェック
			{
				if (!image || maskImage.isEmpty())
				{
					return;
				}

				if (image.size() != maskImage.size())
				{
					return;
				}

				radius = std::max(radius, 0);
			}

			// 2. 処理
			{
				cv::Mat_<cv::Vec3b> matSrc(image.height(), image.width());

				OpenCV_Bridge::ToMatVec3b(image, matSrc);

				cv::Mat_<uint8> matMask(static_cast<int32>(maskImage.height()), static_cast<int32>(maskImage.width()), const_cast<uint8*>(maskImage.data()), static_cast<int32>(maskImage.width()));

				cv::Mat_<cv::Vec3b> matDst;

				cv::inpaint(matSrc, matMask, matDst, radius, cv::INPAINT_TELEA);

				OpenCV_Bridge::FromMat(matDst, result, true);
			}
		}
	}
}
