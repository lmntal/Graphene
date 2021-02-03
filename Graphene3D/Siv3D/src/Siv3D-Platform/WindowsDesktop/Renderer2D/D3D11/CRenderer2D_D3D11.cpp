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

# include <Siv3D/EngineError.hpp>
# include <Siv3D/EngineLog.hpp>
# include <Siv3D/DLL.hpp>
# include <Siv3D/ScopeGuard.hpp>
# include <Siv3D/Mat3x2.hpp>
# include <Siv3D/FloatRect.hpp>
# include <Siv3D/FloatQuad.hpp>
# include <Siv3D/Line.hpp>
# include <Siv3D/Resource.hpp>
# include <Siv3D/Math.hpp>
# include <ConstantBuffer/D3D11/D3D11ConstantBuffer.hpp>
# include <Graphics/D3D11/CGraphics_D3D11.hpp>
# include <Texture/D3D11/CTexture_D3D11.hpp>
# include <Shader/IShader.hpp>
# include <Profiler/IProfiler.hpp>
# include <Siv3DEngine.hpp>
# include <Renderer2D/Vertex2DBuilder.hpp>
# include "CRenderer2D_D3D11.hpp"
# include "D3D11SpriteBatch.hpp"

/*
#	define LOG_COMMAND(...) LOG_TRACE(__VA_ARGS__)
/*/
#	define LOG_COMMAND(...) ((void)0)
//*/

namespace s3d
{
	namespace detail
	{
		static void SetVS(const VertexShader& vs)
		{
			Siv3DEngine::Get<ISiv3DShader>()->setVS(vs.id());
		}

		static void SetPS(const PixelShader& ps)
		{
			Siv3DEngine::Get<ISiv3DShader>()->setPS(ps.id());
		}
	}

	CRenderer2D_D3D11::CRenderer2D_D3D11()
	{

	}

	CRenderer2D_D3D11::~CRenderer2D_D3D11()
	{
		LOG_TRACE(U"CRenderer2D_D3D11::~CRenderer2D_D3D11()");
	}

	void CRenderer2D_D3D11::init(ID3D11Device* device, ID3D11DeviceContext* context)
	{
		LOG_TRACE(U"CRenderer2D_D3D11::init()");

		// device と context のコピー
		{
			m_device = device;
			m_context = context;
		}

		// 標準 VS をロード
		{
			m_standardVS = std::make_unique<D3D11StandardVS2D>();
			m_standardVS->sprite						= VertexShader(Resource(U"engine/shader/sprite.vs"), {{ U"VSConstants2D", 0 }});
			m_standardVS->fullscreen_triangle_resolve	= VertexShader(Resource(U"engine/shader/fullscreen_triangle_resolve.vs"), {});
			m_standardVS->fullscreen_triangle_draw		= VertexShader(Resource(U"engine/shader/fullscreen_triangle_draw.vs"), {});
			if (!m_standardVS->ok())
			{
				throw EngineError(U"CRenderer2D_D3D11::m_standardVS initialization failed");
			}
		}
		
		// 標準 PS をロード
		{
			m_standardPS = std::make_unique<D3D11StandardPS2D>();
			m_standardPS->shape							= PixelShader(Resource(U"engine/shader/shape.ps"), {{ U"PSConstants2D", 0 }});
			m_standardPS->texture						= PixelShader(Resource(U"engine/shader/texture.ps"), {{ U"PSConstants2D", 0 }});
			m_standardPS->square_dot					= PixelShader(Resource(U"engine/shader/square_dot.ps"), {{ U"PSConstants2D", 0 }});
			m_standardPS->round_dot						= PixelShader(Resource(U"engine/shader/round_dot.ps"), {{ U"PSConstants2D", 0 }});
			m_standardPS->sdf							= PixelShader(Resource(U"engine/shader/sdf.ps"), {{ U"PSConstants2D", 0 }});
			m_standardPS->fullscreen_triangle_resolve	= PixelShader(Resource(U"engine/shader/fullscreen_triangle_resolve.ps"), {});
			m_standardPS->fullscreen_triangle_draw		= PixelShader(Resource(U"engine/shader/fullscreen_triangle_draw.ps"), {});
			if (!m_standardPS->setup())
			{
				throw EngineError(U"CRenderer2D_D3D11::m_standardPS initialization failed");
			}
		}

		// 標準 InputLayout を作成
		{
			const D3D11_INPUT_ELEMENT_DESC layout[3] = {
				{ "POSITION", 0, DXGI_FORMAT_R32G32_FLOAT,       0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
				{ "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,       0,  8, D3D11_INPUT_PER_VERTEX_DATA, 0 },
				{ "COLOR",    0, DXGI_FORMAT_R32G32B32A32_FLOAT, 0, 16, D3D11_INPUT_PER_VERTEX_DATA, 0 },
			};

			const ByteArrayView vsView = Siv3DEngine::Get<ISiv3DShader>()->getBinaryView(m_standardVS->sprite.id());

			if (FAILED(m_device->CreateInputLayout(layout, 3, vsView.data(), vsView.size(), &m_inputLayout)))
			{
				throw EngineError(U"ID3D11Device::CreateInputLayout() failed");
			}
		}

		// Batch 管理を初期化
		if (!m_batches.init(device, context))
		{
			throw EngineError(U"D3D11SpriteBatch::init() failed");
		}

		// バッファ作成関数を作成
		m_bufferCreator = [this](IndexType vertexSize, IndexType indexSize)
		{
			return m_batches.getBuffer(vertexSize, indexSize, m_commands);
		};

		// シャドウ画像を作成
		{
			const Image boxShadowImage(Resource(U"engine/texture/box-shadow/256.png"));

			const Array<Image> boxShadowImageMips =
			{
				Image(Resource(U"engine/texture/box-shadow/128.png")),
				Image(Resource(U"engine/texture/box-shadow/64.png")),
				Image(Resource(U"engine/texture/box-shadow/32.png")),
				Image(Resource(U"engine/texture/box-shadow/16.png")),
				Image(Resource(U"engine/texture/box-shadow/8.png")),
			};

			m_boxShadowTexture = std::make_unique<Texture>(boxShadowImage, boxShadowImageMips);

			if (m_boxShadowTexture->isEmpty())
			{
				throw EngineError(U"Failed to create CRenderer2D_D3D11::m_boxShadowTexture");
			}
		}

		LOG_INFO(U"ℹ️ CRenderer2D_D3D11 initialized");
	}

	void CRenderer2D_D3D11::flush()
	{
		ScopeGuard cleanUp = [this]()
		{
			m_currentCustomPS.reset();
			m_batches.reset();
			m_commands.reset();
		};

		m_commands.flush();

		CGraphics_D3D11* const pGraphics = dynamic_cast<CGraphics_D3D11* const>(Siv3DEngine::Get<ISiv3DGraphics>());
		CTexture_D3D11* const pTexture = dynamic_cast<CTexture_D3D11* const>(Siv3DEngine::Get<ISiv3DTexture>());

		if (!pGraphics || !pTexture)
		{
			return;
		}

		m_context->IASetInputLayout(m_inputLayout.Get());
		detail::SetVS(m_standardVS->sprite);

		m_context->VSSetConstantBuffers(0, 1, m_vsConstants2D.base()._detail()->getBufferPtr());
		m_context->PSSetConstantBuffers(0, 1, m_psConstants2D.base()._detail()->getBufferPtr());

		Size currentRenderTargetSize = pGraphics->getSceneSize();
		Mat3x2 transform = Mat3x2::Identity();
		Mat3x2 screenMat = Mat3x2::Screen(currentRenderTargetSize);	
		BatchInfo batchInfo;
		size_t profile_drawcalls = 0, profile_vertices = 0;

		LOG_COMMAND(U"--Renderer2D commands--");

		for (auto[command, index] : m_commands.getList())
		{
			switch (command)
			{
			case RendererCommand::SetBuffers:
				{
					LOG_COMMAND(U"SetBuffers[{}]"_fmt(index));

					m_batches.setBuffers();
					break;
				}
			case RendererCommand::UpdateBuffers:
				{
					batchInfo = m_batches.updateBuffers(index);

					LOG_COMMAND(U"UpdateBuffers[{}] BatchInfo(indexCount = {}, startIndexLocation = {}, baseVertexLocation = {})"_fmt(
						index, batchInfo.indexCount, batchInfo.startIndexLocation, batchInfo.baseVertexLocation));
					break;
				}
			case RendererCommand::Draw:
				{
					m_vsConstants2D._update_if_dirty();
					m_psConstants2D._update_if_dirty();

					const DrawCommand& draw = m_commands.getDraw(index);
					const uint32 indexCount = draw.indexCount;
					const uint32 startIndexLocation = batchInfo.startIndexLocation;

					m_context->DrawIndexed(indexCount, startIndexLocation, batchInfo.baseVertexLocation);
					batchInfo.startIndexLocation += indexCount;

					++profile_drawcalls;
					profile_vertices += indexCount;

					LOG_COMMAND(U"Draw[{}] indexCount = {}, startIndexLocation = {}"_fmt(index, indexCount, startIndexLocation));
					break;
				}
			case RendererCommand::ColorMul:
				{
					m_vsConstants2D->colorMul = m_commands.getColorMul(index);

					LOG_COMMAND(U"ColorMul[{}] {}"_fmt(index, m_cbSprite0->colorMul));
					break;
				}
			case RendererCommand::ColorAdd:
				{
					m_psConstants2D->colorAdd = m_commands.getColorAdd(index);

					LOG_COMMAND(U"ColorAdd[{}] {}"_fmt(index, m_cbSprite1->colorAdd));
					break;
				}
			case RendererCommand::BlendState:
				{
					const auto& blendState = m_commands.getBlendState(index);
					pGraphics->getBlendState()->set(blendState);
					LOG_COMMAND(U"BlendState[{}]"_fmt(index));
					break;
				}
			case RendererCommand::RasterizerState:
				{
					const auto& rasterizerState = m_commands.getRasterizerState(index);
					pGraphics->getRasterizerState()->set(rasterizerState);
					LOG_COMMAND(U"RasterizerState[{}]"_fmt(index));
					break;
				}
			case RendererCommand::PSSamplerState0:
			case RendererCommand::PSSamplerState1:
			case RendererCommand::PSSamplerState2:
			case RendererCommand::PSSamplerState3:
			case RendererCommand::PSSamplerState4:
			case RendererCommand::PSSamplerState5:
			case RendererCommand::PSSamplerState6:
			case RendererCommand::PSSamplerState7:
				{
					const uint32 slot = FromEnum(command) - FromEnum(RendererCommand::PSSamplerState0);
					const auto& samplerState = m_commands.getPSSamplerState(slot, index);
					pGraphics->getSamplerState()->setPS(slot, samplerState);
					LOG_COMMAND(U"PSSamplerState{}[{}] "_fmt(slot, index));
					break;
				}
			case RendererCommand::Transform:
				{
					transform = m_commands.getCombinedTransform(index);
					const Mat3x2 matrix = transform * screenMat;
					m_vsConstants2D->transform[0].set(matrix._11, matrix._12, matrix._31, matrix._32);
					m_vsConstants2D->transform[1].set(matrix._21, matrix._22, 0.0f, 1.0f);

					LOG_COMMAND(U"Transform[{}] {}"_fmt(index, matrix));
					break;
				}
			case RendererCommand::SetPS:
				{
					const auto& psID = m_commands.getPS(index);

					if (psID == PixelShaderID::InvalidValue())
					{
						;// [Siv3D ToDo] set null
					}
					else
					{
						Siv3DEngine::Get<ISiv3DShader>()->setPS(psID);
					}

					LOG_COMMAND(U"SetPS[{}] "_fmt(index));
					break;
				}
			case RendererCommand::SetCB:
				{
					auto& cb = m_commands.getCB(index);
					const __m128* p = m_commands.getConstantsPtr(cb.offset);

					if (cb.num_vectors)
					{
						if (cb.stage == ShaderStage::Vertex)
						{
							m_context->VSSetConstantBuffers(cb.slot, 1, cb.cbBase._detail()->getBufferPtr());
						}
						else if (cb.stage == ShaderStage::Pixel)
						{
							m_context->PSSetConstantBuffers(cb.slot, 1, cb.cbBase._detail()->getBufferPtr());							
						}

						cb.cbBase._internal_update(p, cb.num_vectors * 16);
					}

					LOG_COMMAND(U"SetCB[{}] (stage = {}, slot = {}, offset = {}, num_vectors = {})"_fmt(
						index, FromEnum(cb.stage), cb.slot, cb.offset, cb.num_vectors));
					break;
				}
			case RendererCommand::SetRT:
				{
					const auto& rt = m_commands.getRT(index);
					
					if (rt) // 通常と異なる RenderTexture
					{
						ID3D11RenderTargetView* const rtv = pTexture->getRTV(rt->id());
						pGraphics->getRenderTarget()->setRenderTarget(rtv);
					}
					else // [BackBuffer]
					{
						pGraphics->getRenderTarget()->setRenderTarget(RenderTargetType::MultiSampledScene);
					}

					LOG_COMMAND(U"SetRT[{}]"_fmt(index));
					break;
				}
			case RendererCommand::ScissorRect:
				{
					const auto& scissorRect = m_commands.getScissorRect(index);
					pGraphics->getRasterizerState()->setScissorRect(scissorRect);
					LOG_COMMAND(U"ScissorRect[{}] {}"_fmt(index, scissorRect));
					break;
				}
			case RendererCommand::Viewport:
				{
					const auto& viewport = m_commands.getViewport(index);
					D3D11_VIEWPORT vp;
					vp.MinDepth = 0.0f;
					vp.MaxDepth = 1.0f;

					if (viewport)
					{
						vp.TopLeftX	= static_cast<float>(viewport->x);
						vp.TopLeftY	= static_cast<float>(viewport->y);
						vp.Width	= static_cast<float>(viewport->w);
						vp.Height	= static_cast<float>(viewport->h);
					}
					else
					{
						vp.TopLeftX = 0;
						vp.TopLeftY = 0;
						vp.Width	= static_cast<float>(currentRenderTargetSize.x);
						vp.Height	= static_cast<float>(currentRenderTargetSize.y);
					}

					m_context->RSSetViewports(1, &vp);

					screenMat = Mat3x2::Screen(vp.Width, vp.Height);
					const Mat3x2 matrix = transform * screenMat;
					m_vsConstants2D->transform[0].set(matrix._11, matrix._12, matrix._31, matrix._32);
					m_vsConstants2D->transform[1].set(matrix._21, matrix._22, 0.0f, 1.0f);

					LOG_COMMAND(U"Viewport[{}] (TopLeftX = {}, TopLeftY = {}, Width = {}, Height = {}, MinDepth = {}, MaxDepth = {})"_fmt(index,
						vp.TopLeftX, vp.TopLeftY, vp.Width, vp.Height, vp.MinDepth, vp.MaxDepth));
					break;
				}
			case RendererCommand::PSTexture0:
			case RendererCommand::PSTexture1:
			case RendererCommand::PSTexture2:
			case RendererCommand::PSTexture3:
			case RendererCommand::PSTexture4:
			case RendererCommand::PSTexture5:
			case RendererCommand::PSTexture6:
			case RendererCommand::PSTexture7:
				{
					const uint32 slot = FromEnum(command) - FromEnum(RendererCommand::PSTexture0);
					const auto& textureID = m_commands.getPSTexture(slot, index);

					if (textureID == TextureID::InvalidValue())
					{
						ID3D11ShaderResourceView* nullAttach[1] = { nullptr };
						m_context->PSSetShaderResources(slot, 1, nullAttach);
					}
					else
					{
						m_context->PSSetShaderResources(slot, 1, pTexture->getSRVPtr(textureID));
					}
					
					LOG_COMMAND(U"PSTexture{}[{}] "_fmt(slot, index));
					break;
				}
			case RendererCommand::SDFParam:
				{
					m_psConstants2D->sdfParam = m_commands.getSdfParam(index);

					LOG_COMMAND(U"SDFParam[{}] {}"_fmt(index, m_psConstants2D->sdfParam));
					break;
				}
			case RendererCommand::InternalPSConstants:
				{
					m_psConstants2D->internalParam = m_commands.getInternalPSConstants(index);

					LOG_COMMAND(U"InternalPSConstants[{}] {}"_fmt(index, m_psConstants2D->internalParam));
					break;
				}
			default:
				{
					LOG_COMMAND(U"???[{}] "_fmt(index));
				}
			}
		}

		LOG_COMMAND(U"--({} commands)--"_fmt(m_commands.getList().size()));

		Siv3DEngine::Get<ISiv3DProfiler>()->reportDrawcalls(profile_drawcalls, profile_vertices / 3);
	}

	std::pair<float, FloatRect> CRenderer2D_D3D11::getLetterboxingTransform() const
	{
		CGraphics_D3D11* const pGraphics = dynamic_cast<CGraphics_D3D11* const>(Siv3DEngine::Get<ISiv3DGraphics>());

		const Float2 sceneSize = pGraphics->getSceneSize();
		const Float2 backBufferSize = pGraphics->getBackBufferSize();

		const float sx = backBufferSize.x / sceneSize.x;
		const float sy = backBufferSize.y / sceneSize.y;
		const float s = std::min(sx, sy);

		if (sx <= sy)
		{
			const float offsetY = (backBufferSize.y - sceneSize.y * s) * 0.5f;
			return{ s, FloatRect(0.0f, offsetY,
				static_cast<float>(backBufferSize.x), 
				static_cast<float>(backBufferSize.y - offsetY * 2.0f)) };
		}
		else
		{
			const float offsetX = (backBufferSize.x - sceneSize.x * s) * 0.5f;
			return{ s, FloatRect(offsetX, 0.0f,
				static_cast<float>(backBufferSize.x - offsetX * 2.0f),
				static_cast<float>(backBufferSize.y)) };
		}
	}

	void CRenderer2D_D3D11::drawFullscreen(const bool multisample)
	{
		CGraphics_D3D11* const pGraphics = dynamic_cast<CGraphics_D3D11* const>(Siv3DEngine::Get<ISiv3DGraphics>());

		if (!pGraphics)
		{
			return;
		}

		const Float2 sceneSize = pGraphics->getSceneSize();
		auto[s, viewRect] = getLetterboxingTransform();
		
		const CD3D11_VIEWPORT viewport(
			viewRect.left, viewRect.top,
			viewRect.right,
			viewRect.bottom);
		m_context->RSSetViewports(1, &viewport);

		if (multisample)
		{
			detail::SetVS(m_standardVS->fullscreen_triangle_resolve);
			detail::SetPS(m_standardPS->fullscreen_triangle_resolve);

			m_cbFullscreenTriangle0->texScale.set(sceneSize, 0.0f, 0.0f);
			m_cbFullscreenTriangle0._update_if_dirty();
			m_context->VSSetConstantBuffers(0, 1, m_cbFullscreenTriangle0.base()._detail()->getBufferPtr());

			pGraphics->getBlendState()->set(BlendState::Opaque);
			pGraphics->getRasterizerState()->set(RasterizerState::Default2D);
			pGraphics->getSamplerState()->setPS(0, SamplerState::Default2D);
		}
		else
		{
			detail::SetVS(m_standardVS->fullscreen_triangle_draw);
			detail::SetPS(m_standardPS->fullscreen_triangle_draw);

			pGraphics->getBlendState()->set(BlendState::Opaque);
			pGraphics->getRasterizerState()->set(RasterizerState::Default2D);
			pGraphics->getSamplerState()->setPS(0, SamplerState::Default2D);
		}

		// Null VB, IB
		m_context->IASetVertexBuffers(0, 0, nullptr, nullptr, nullptr);
		m_context->IASetIndexBuffer(nullptr, DXGI_FORMAT_UNKNOWN, 0);
		m_context->IASetInputLayout(nullptr);
		m_context->Draw(3, 0);

		Siv3DEngine::Get<ISiv3DProfiler>()->reportDrawcalls(1, 1);
	}

	void CRenderer2D_D3D11::setColorMul(const Float4& color)
	{
		m_commands.pushColorMul(color);
	}

	ColorF CRenderer2D_D3D11::getColorMul() const
	{
		return ColorF(m_commands.getCurrentColorMul());
	}

	void CRenderer2D_D3D11::setColorAdd(const Float4& color)
	{
		m_commands.pushColorAdd(color);
	}

	ColorF CRenderer2D_D3D11::getColorAdd() const
	{
		return ColorF(m_commands.getCurrentColorAdd());
	}

	void CRenderer2D_D3D11::setBlendState(const BlendState& state)
	{
		m_commands.pushBlendState(state);
	}

	BlendState CRenderer2D_D3D11::getBlendState() const
	{
		return m_commands.getCurrentBlendState();
	}

	void CRenderer2D_D3D11::setRasterizerState(const RasterizerState& state)
	{
		m_commands.pushRasterizerState(state);
	}

	RasterizerState CRenderer2D_D3D11::getRasterizerState() const
	{
		return m_commands.getCurrentRasterizerState();
	}

	void CRenderer2D_D3D11::setPSSamplerState(const uint32 slot, const SamplerState& state)
	{
		m_commands.pushPSSamplerState(state, slot);
	}

	SamplerState CRenderer2D_D3D11::getPSSamplerState(const uint32 slot) const
	{
		return m_commands.getPSCurrentSamplerState(slot);
	}

	void CRenderer2D_D3D11::setLocalTransform(const Mat3x2& matrix)
	{
		return m_commands.pushLocalTransform(matrix);
	}

	const Mat3x2& CRenderer2D_D3D11::getLocalTransform() const
	{
		return m_commands.getCurrentLocalTransform();
	}

	void CRenderer2D_D3D11::setCameraTransform(const Mat3x2& matrix)
	{
		return m_commands.pushCameraTransform(matrix);
	}

	const Mat3x2& CRenderer2D_D3D11::getCameraTransform() const
	{
		return m_commands.getCurrentCameraTransform();
	}

	float CRenderer2D_D3D11::getMaxScaling() const
	{
		return m_commands.getCurrentMaxScaling();
	}

	void CRenderer2D_D3D11::setScissorRect(const Rect& rect)
	{
		m_commands.pushScissorRect(rect);
	}

	Rect CRenderer2D_D3D11::getScissorRect() const
	{
		return m_commands.getCurrentScissorRect();
	}

	void CRenderer2D_D3D11::setViewport(const Optional<Rect>& viewport)
	{
		m_commands.pushViewport(viewport);
	}

	Optional<Rect> CRenderer2D_D3D11::getViewport() const
	{
		return m_commands.getCurrentViewport();
	}

	void CRenderer2D_D3D11::setSDFParameters(const Float4& parameters)
	{
		m_commands.pushSdfParam(parameters);
	}

	Float4 CRenderer2D_D3D11::getSDFParameters() const
	{
		return m_commands.getCurrentSdfParam();
	}

	void CRenderer2D_D3D11::setPSTexture(const uint32 slot, const Optional<Texture>& texture)
	{
		if (texture)
		{
			m_commands.pushPSTexture(slot, texture.value());
		}
		else
		{
			m_commands.pushPSTextureUnbound(slot);
		}
	}

	void CRenderer2D_D3D11::setCustomPS(const Optional<PixelShader>& ps)
	{
		if (ps && !ps->isEmpty())
		{
			m_currentCustomPS = ps.value();
			m_commands.pushCustomPS(ps.value());
		}
		else
		{
			m_currentCustomPS.reset();
		}
	}

	Optional<PixelShader> CRenderer2D_D3D11::getCustomPS() const
	{
		return m_currentCustomPS;
	}

	void CRenderer2D_D3D11::setConstant(const ShaderStage stage, const uint32 slot, const s3d::detail::ConstantBufferBase& buffer, const float* data, const uint32 num_vectors)
	{
		m_commands.pushCB(stage, slot, buffer, data, num_vectors);
	}

	void CRenderer2D_D3D11::setInternalConstantBufferValue(const ShaderStage stage, const Float4& value)
	{
		if (stage == ShaderStage::Vertex)
		{
			// [Siv3D ToDo] v0.4.1 では未実装
		}
		else if (stage == ShaderStage::Pixel)
		{
			m_commands.pushInternalPSConstants(value);
		}
	}

	void CRenderer2D_D3D11::setRT(const Optional<RenderTexture>& rt)
	{
		if (rt)
		{
			bool hasChanged = false;

			const TextureID textureID = rt->id();
			const auto& currentPSTextures = m_commands.getCurrentPSTextures();

			for (uint32 slot = 0; slot < currentPSTextures.size(); ++slot)
			{
				if (currentPSTextures[slot] == textureID)
				{
					m_commands.pushPSTexture(slot, Texture());
					hasChanged = true;
				}
			}

			if (hasChanged)
			{
				m_commands.flush();
			}
		}
		
		m_commands.pushRT(rt);
	}

	Optional<RenderTexture> CRenderer2D_D3D11::getRT() const
	{
		return m_commands.getCurrentRT();
	}

	void CRenderer2D_D3D11::addLine(const LineStyle& style, const Float2& begin, const Float2& end, const float thickness, const Float4(&colors)[2])
	{
		if (style.isSquareCap())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildSquareCappedLine(m_bufferCreator, begin, end, thickness, colors))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isRoundCap())
		{
			float startAngle = 0.0f;

			if (const uint16 indexCount = Vertex2DBuilder::BuildRoundCappedLine(m_bufferCreator, begin, end, thickness, colors, startAngle))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);

				const float thicknessHalf = thickness * 0.5f;
				addCirclePie(begin, thicknessHalf, startAngle, Math::Constants::PiF, colors[0]);
				addCirclePie(end, thicknessHalf, startAngle + Math::Constants::PiF, Math::Constants::PiF, colors[1]);
			}
		}
		else if (style.isNoCap())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildUncappedLine(m_bufferCreator, begin, end, thickness, colors))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isSquareDot())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildSquareDotLine(m_bufferCreator, begin, end, thickness, colors, static_cast<float>(style.dotOffset), getMaxScaling()))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->square_dotID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isRoundDot())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildRoundDotLine(m_bufferCreator, begin, end, thickness, colors, static_cast<float>(style.dotOffset), style.hasAlignedDot))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->round_dotID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
	}

	void CRenderer2D_D3D11::addTriangle(const Float2(&pts)[3], const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTriangle(m_bufferCreator, pts, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addTriangle(const Float2(&pts)[3], const Float4(&colors)[3])
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTriangle(m_bufferCreator, pts, colors))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addRect(const FloatRect& rect, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildRect(m_bufferCreator, rect, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addRect(const FloatRect& rect, const Float4(&colors)[4])
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildRect(m_bufferCreator, rect, colors))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addRectFrame(const FloatRect& rect, const float thickness, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildRectFrame(m_bufferCreator, rect, thickness, color, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addRectFrame(const FloatRect& rect, const float thickness, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildRectFrame(m_bufferCreator, rect, thickness, innerColor, outerColor))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCircle(const Float2& center, const float r, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCircle(m_bufferCreator, center, r, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCircleFrame(const Float2& center, const float rInner, const float thickness, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCircleFrame(m_bufferCreator, center, rInner, thickness, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCirclePie(const Float2& center, const float r, const float startAngle, const float angle, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCirclePie(m_bufferCreator, center, r, startAngle, angle, color, color, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCirclePie(const Float2& center, const float r, const float startAngle, const float angle, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCirclePie(m_bufferCreator, center, r, startAngle, angle, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCircleArc(const Float2& center, const float rInner, const float startAngle, float angle, const float thickness, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCircleArc(m_bufferCreator, center, rInner, startAngle, angle, thickness, color, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addCircleArc(const Float2& center, const float rInner, const float startAngle, float angle, const float thickness, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildCircleArc(m_bufferCreator, center, rInner, startAngle, angle, thickness, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addEllipse(const Float2& center, const float a, const float b, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildEllipse(m_bufferCreator, center, a, b, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addEllipseFrame(const Float2& center, const float aInner, const float bInner, const float thickness, const Float4& innerColor, const Float4& outerColor)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildEllipseFrame(m_bufferCreator, center, aInner, bInner, thickness, innerColor, outerColor, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addQuad(const FloatQuad& quad, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildQuad(m_bufferCreator, quad, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addQuad(const FloatQuad& quad, const Float4(&colors)[4])
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildQuad(m_bufferCreator, quad, colors))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addRoundRect(const FloatRect& rect, const float w, const float h, const float r, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildRoundRect(m_bufferCreator, rect, w, h, r, color, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addLineString(const LineStyle& style, const Vec2* pts, const uint16 size, const Optional<Float2>& offset, const float thickness, const bool inner, const Float4& color, const bool isClosed)
	{
		if (style.isSquareCap() || (style.isRoundCap() && isClosed))
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildSquareCappedLineString(m_bufferCreator, pts, size, offset, thickness, inner, color, isClosed, getMaxScaling()))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isRoundCap())
		{
			float startAngle = 0.0f, endAngle = 0.0f;

			if (const uint16 indexCount = Vertex2DBuilder::BuildRoundCappedLineString(m_bufferCreator, pts, size, offset, thickness, inner, color, getMaxScaling(), startAngle, endAngle))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);
			
				const float thicknessHalf = thickness * 0.5f;
				addCirclePie(*pts, thicknessHalf, startAngle, Math::Constants::PiF, color);
				addCirclePie(*(pts + size - 1), thicknessHalf, endAngle, Math::Constants::PiF, color);
			}
		}
		else if (style.isNoCap())
		{
			float startAngle = 0.0f, endAngle = 0.0f;

			if (const uint16 indexCount = Vertex2DBuilder::BuildRoundCappedLineString(m_bufferCreator, pts, size, offset, thickness, inner, color, getMaxScaling(), startAngle, endAngle))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->shapeID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isSquareDot())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildDotLineString(m_bufferCreator, pts, size, offset, thickness, color, isClosed, true, static_cast<float>(style.dotOffset), false, getMaxScaling()))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->square_dotID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
		else if (style.isRoundDot())
		{
			if (const uint16 indexCount = Vertex2DBuilder::BuildDotLineString(m_bufferCreator, pts, size, offset, thickness, color, isClosed, false, static_cast<float>(style.dotOffset), style.hasAlignedDot, getMaxScaling()))
			{
				if (!m_currentCustomPS)
				{
					m_commands.pushStandardPS(m_standardPS->round_dotID);
				}
				m_commands.pushDraw(indexCount);
			}
		}
	}

	void CRenderer2D_D3D11::addShape2D(const Array<Float2>& vertices, const Array<uint16>& indices, const Optional<Float2>& offset, const Float4& color)
	{
		if (const uint16 count = Vertex2DBuilder::BuildShape2D(m_bufferCreator, vertices, indices, offset, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(count);
		}
	}

	void CRenderer2D_D3D11::addShape2DTransformed(const Array<Float2>& vertices, const Array<uint16>& indices, const float s, const float c, const Float2& offset, const Float4& color)
	{
		if (const uint16 count = Vertex2DBuilder::BuildShape2DTransformed(m_bufferCreator, vertices, indices, s, c, offset, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(count);
		}
	}

	void CRenderer2D_D3D11::addShape2DFrame(const Float2* pts, const uint16 size, const float thickness, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildShape2DFrame(m_bufferCreator, pts, size, thickness, color, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addSprite(const Vertex2D* vertices, const size_t vertexCount, const uint16* indices, const size_t indexCount)
	{
		if (const uint16 count = Vertex2DBuilder::BuildSprite(m_bufferCreator, vertices, vertexCount, indices, indexCount))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(count);
		}
	}

	void CRenderer2D_D3D11::addSprite(const Sprite& sprite, const uint16 startIndex, const uint16 indexCount)
	{
		if (const uint16 count = Vertex2DBuilder::BuildSprite(m_bufferCreator, sprite, startIndex, indexCount))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(m_standardPS->shapeID);
			}
			m_commands.pushDraw(count);
		}
	}

	void CRenderer2D_D3D11::addSprite(const Texture& texture, const Sprite& sprite, const uint16 startIndex, const uint16 indexCount)
	{
		if (const uint16 count = Vertex2DBuilder::BuildSprite(m_bufferCreator, sprite, startIndex, indexCount))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(count);
		}
	}

	void CRenderer2D_D3D11::addTextureRegion(const Texture& texture, const FloatRect& rect, const FloatRect& uv, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTextureRegion(m_bufferCreator, rect, uv, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addTextureRegion(const Texture& texture, const FloatRect& rect, const FloatRect& uv, const Float4(&colors)[4])
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTextureRegion(m_bufferCreator, rect, uv, colors))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addTexturedCircle(const Texture& texture, const Circle& circle, const FloatRect& uv, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTexturedCircle(m_bufferCreator, circle, uv, color, getMaxScaling()))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addTexturedQuad(const Texture& texture, const FloatQuad& quad, const FloatRect& uv, const Float4& color)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTexturedQuad(m_bufferCreator, quad, uv, color))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(indexCount);
		}
	}

	void CRenderer2D_D3D11::addTexturedParticles(const Texture& texture, const Array<Particle2D>& particles,
		ParticleSystem2DParameters::SizeOverLifeTimeFunc sizeOverLifeTimeFunc,
		ParticleSystem2DParameters::ColorOverLifeTimeFunc colorOverLifeTimeFunc)
	{
		if (const uint16 indexCount = Vertex2DBuilder::BuildTexturedParticles(m_bufferCreator, particles, sizeOverLifeTimeFunc, colorOverLifeTimeFunc))
		{
			if (!m_currentCustomPS)
			{
				m_commands.pushStandardPS(texture.isSDF() ? m_standardPS->sdfID : m_standardPS->textureID);
			}
			m_commands.pushPSTexture(0, texture);
			m_commands.pushDraw(indexCount);
		}
	}

	const Texture& CRenderer2D_D3D11::getBoxShadowTexture() const
	{
		return *m_boxShadowTexture;
	}
}
