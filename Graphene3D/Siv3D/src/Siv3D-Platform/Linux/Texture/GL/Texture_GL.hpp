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
# include <Siv3D/Image.hpp>
# include <Siv3D/Texture.hpp>
# include <Siv3D/TextureFormat.hpp>
# include <GL/glew.h>
# include <GLFW/glfw3.h>

namespace s3d
{
	class Texture_GL
	{
	private:
		
		enum class TextureType
		{
			// 通常テクスチャ
			// [メインテクスチャ]
			Normal,
			
			// 動的テクスチャ
			// [メインテクスチャ]
			Dynamic,
			
			// レンダーテクスチャ
			// [メインテクスチャ]<-[フレームバッファ]
			Render,
			
			// マルチサンプル・レンダーテクスチャ
			// [マルチサンプル・テクスチャ]<-[フレームバッファ], [メインテクスチャ]<-[resolved フレームバッファ]
			MSRender,
		};
		
		// [メインテクスチャ]
		GLuint m_texture = 0;
		
		// [マルチサンプル・テクスチャ]
		GLuint m_multiSampledTexture = 0;
		
		// [フレームバッファ]
		GLuint m_frameBuffer = 0;
		
		// [resolved フレームバッファ]
		GLuint m_resolvedFrameBuffer = 0;
		
		Size m_size = { 0, 0 };
		
		TextureFormat m_format = TextureFormat::Unknown;
		
		TextureDesc m_textureDesc = TextureDesc::Unmipped;
		
		TextureType m_type = TextureType::Normal;

		bool m_initialized = false;
		
	public:
		
		struct Null {};
		struct Dynamic {};
		struct Render {};
		struct MSRender {};
		
		Texture_GL() = default;
		
		Texture_GL(Null);
		
		Texture_GL(const Image& image, TextureDesc desc);
		
		Texture_GL(const Image& image, const Array<Image>& mipmaps, TextureDesc desc);
		
		Texture_GL(Dynamic, const Size& size, const void* pData, uint32 stride, const TextureFormat& format, TextureDesc desc);
		
		Texture_GL(Render, const Size& size, const TextureFormat& format, TextureDesc desc);
		
		Texture_GL(Render, const Image& image, const TextureFormat& format, TextureDesc desc);
		
		Texture_GL(Render, const Grid<float>& image, const TextureFormat& format, TextureDesc desc);

		Texture_GL(Render, const Grid<Float2>& image, const TextureFormat& format, TextureDesc desc);

		Texture_GL(Render, const Grid<Float4>& image, const TextureFormat& format, TextureDesc desc);
		
		Texture_GL(MSRender, const Size& size, const TextureFormat& format, TextureDesc desc);
		
		~Texture_GL();
		
		bool isInitialized() const noexcept;
		
		GLuint getTexture() const noexcept;
		
		GLuint getFrameBuffer() const noexcept;
		
		Size getSize() const noexcept;
		
		TextureDesc getDesc() const noexcept;
		
		TextureFormat getFormat() const noexcept;
		
		// レンダーテクスチャを指定した色でクリアする
		void clearRT(const ColorF& color);
		
		// レンダーテクスチャの内容を Image にコピーする
		void readRT(Image& image);
		
		// レンダーテクスチャの内容を Grid にコピーする
		void readRT(Grid<float>& image);
		
		// レンダーテクスチャの内容を Grid にコピーする
		void readRT(Grid<Float2>& image);
		
		// レンダーテクスチャの内容を Grid にコピーする
		void readRT(Grid<Float4>& image);
		
		void resolveMSRT();
		
		// 動的テクスチャを指定した色で塗りつぶす
		bool fill(const ColorF& color, bool wait);
		
		bool fillRegion(const ColorF& color, const Rect& rect);
		
		bool fill(const void* src, uint32 stride, bool wait);
		
		bool fillRegion(const void* src, uint32 stride, const Rect& rect, bool wait);
	};
}
