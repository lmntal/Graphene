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

# include <Siv3D/Fwd.hpp>
SIV3D_DISABLE_MSVC_WARNINGS_PUSH(5054)
# define RAPIDJSON_SSE2
# include <rapidjson/rapidjson.h>
# include <rapidjson/document.h>
SIV3D_DISABLE_MSVC_WARNINGS_POP()
# include <Siv3D/JSONReader.hpp>
# include <Siv3D/TextReader.hpp>

namespace s3d
{
	namespace detail
	{
		struct JSONArrayIteratorDetail
		{
			const rapidjson::GenericValue<rapidjson::UTF32<char32>>* pValue = nullptr;

			JSONArrayIteratorDetail() = default;

			explicit constexpr JSONArrayIteratorDetail(const rapidjson::GenericValue<rapidjson::UTF32<char32>>* p) noexcept
				: pValue(p) {}
		};

		struct JSONMemberIteratorDetail
		{
			rapidjson::GenericValue<rapidjson::UTF32<char32>>::ConstMemberIterator it;

			JSONMemberIteratorDetail() = default;

			JSONMemberIteratorDetail(const rapidjson::GenericValue<rapidjson::UTF32<char32>>::ConstMemberIterator& _it)
				: it(_it) {}
		};

		struct JSONValueDetail
		{
			Optional<const rapidjson::GenericValue<rapidjson::UTF32<char32>>&> value;

			JSONValueDetail() = default;

			JSONValueDetail(const Optional<const rapidjson::GenericValue<rapidjson::UTF32<char32>>&>& _value)
				: value(_value) {}
		};

		struct JSONDocumentDetail
		{
			rapidjson::GenericDocument<rapidjson::UTF32<char32>> document;
		};
	}

	////////////////////////////////
	//
	//	JSONArrayIterator
	//

	JSONArrayIterator::JSONArrayIterator()
		: m_detail(std::make_shared<detail::JSONArrayIteratorDetail>())
	{

	}

	JSONArrayIterator::JSONArrayIterator(const detail::JSONArrayIteratorDetail& p)
		: m_detail(std::make_shared<detail::JSONArrayIteratorDetail>(p.pValue))
	{

	}

	JSONArrayIterator JSONArrayIterator::operator++()
	{
		++m_detail->pValue;

		return JSONArrayIterator(*m_detail);
	}

	JSONArrayIterator JSONArrayIterator::operator++(int)
	{
		const detail::JSONArrayIteratorDetail tmp(m_detail->pValue);

		++m_detail->pValue;

		return JSONArrayIterator(tmp);
	}

	JSONArrayIterator JSONArrayIterator::operator +(const size_t index) const
	{
		return JSONArrayIterator(detail::JSONArrayIteratorDetail(m_detail->pValue + index));
	}

	JSONValue JSONArrayIterator::operator *() const
	{
		return JSONValue(Optional<const rapidjson::GenericValue<rapidjson::UTF32<char32>>&>(*(m_detail->pValue)));
	}

	bool JSONArrayIterator::operator ==(const JSONArrayIterator& other) const noexcept
	{
		return m_detail->pValue == other.m_detail->pValue;
	}

	bool JSONArrayIterator::operator !=(const JSONArrayIterator& other) const noexcept
	{
		return m_detail->pValue != other.m_detail->pValue;
	}

	////////////////////////////////
	//
	//	JSONObjectIterator
	//

	JSONObjectIterator::JSONObjectIterator()
		: m_detail(std::make_shared<detail::JSONMemberIteratorDetail>())
	{

	}

	JSONObjectIterator::JSONObjectIterator(const detail::JSONMemberIteratorDetail& it)
		: m_detail(std::make_shared<detail::JSONMemberIteratorDetail>(it.it))
	{

	}

	JSONObjectIterator JSONObjectIterator::operator ++()
	{
		++m_detail->it;

		return JSONObjectIterator(detail::JSONMemberIteratorDetail(*m_detail));
	}

	JSONObjectIterator JSONObjectIterator::operator ++(int)
	{
		const detail::JSONMemberIteratorDetail tmp(m_detail->it);

		++m_detail->it;

		return JSONObjectIterator(tmp);
	}

	JSONObjectMember JSONObjectIterator::operator *() const
	{
		return{ String(m_detail->it->name.GetString(), m_detail->it->name.GetStringLength()),
			JSONValue(Optional<const rapidjson::GenericValue<rapidjson::UTF32<char32>>&>(m_detail->it->value)) };
	}

	bool JSONObjectIterator::operator ==(const JSONObjectIterator& other) const noexcept
	{
		return m_detail->it == other.m_detail->it;
	}

	bool JSONObjectIterator::operator !=(const JSONObjectIterator& other) const noexcept
	{
		return m_detail->it != other.m_detail->it;
	}

	////////////////////////////////
	//
	//	JSONArrayView
	//

	JSONValue JSONArrayView::operator [](size_t index) const
	{
		return *(m_begin + index);
	}

	////////////////////////////////
	//
	//	JSONValue
	//

	JSONValue::JSONValue()
		: m_detail(std::make_shared<detail::JSONValueDetail>())
	{

	}

	JSONValue::JSONValue(const detail::JSONValueDetail& value)
		: m_detail(std::make_shared<detail::JSONValueDetail>(value.value))
	{

	}

	JSONValue JSONValue::operator [](const String& path) const
	{
		if (isEmpty())
		{
			return JSONValue();
		}

		auto value = m_detail->value;

		for (const auto& p : path.split(U'.'))
		{
			if (!value->HasMember(p.c_str()))
			{
				return JSONValue();
			}

			value = Optional<const rapidjson::GenericValue<rapidjson::UTF32<char32>>&>((*value)[p.c_str()]);
		}

		return JSONValue(detail::JSONValueDetail(value));

		/*
		if (isEmpty() || !m_detail->value->HasMember(path.c_str()))
		{
			return JSONValue();
		}

		return JSONValue(detail::JSONValueDetail((*m_detail->value)[path.c_str()]));
		*/
	}

	bool JSONValue::isEmpty() const
	{
		return !m_detail->value.has_value();
	}

	JSONValueType JSONValue::getType() const
	{
		if (isEmpty())
		{
			return JSONValueType::Empty;
		}

		switch (m_detail->value->GetType())
		{
		case rapidjson::kNullType:
			return JSONValueType::Null;
		case rapidjson::kFalseType:
		case rapidjson::kTrueType:
			return JSONValueType::Bool;
		case rapidjson::kObjectType:
			return JSONValueType::Object;
		case rapidjson::kArrayType:
			return JSONValueType::Array;
		case rapidjson::kStringType:
			return JSONValueType::String;
		default:
			return JSONValueType::Number;
		}
	}

	bool JSONValue::isNull() const
	{
		return getType() == JSONValueType::Null;
	}

	bool JSONValue::isBool() const
	{
		return getType() == JSONValueType::Bool;
	}

	bool JSONValue::isObject() const
	{
		return getType() == JSONValueType::Object;
	}

	bool JSONValue::isArray() const
	{
		return getType() == JSONValueType::Array;
	}

	bool JSONValue::isString() const
	{
		return getType() == JSONValueType::String;
	}

	bool JSONValue::isNumber() const
	{
		return getType() == JSONValueType::Number;
	}

	size_t JSONValue::memberCount() const
	{
		if (!isObject())
		{
			return 0;
		}

		return m_detail->value->MemberCount();
	}

	bool JSONValue::hasMember(const String& name) const
	{
		if (!isObject())
		{
			return false;
		}

		return m_detail->value->HasMember(name.c_str());
	}

	JSONObjectView JSONValue::objectView() const
	{
		if (!isObject())
		{
			return JSONObjectView();
		}

		return JSONObjectView(
			JSONObjectIterator(detail::JSONMemberIteratorDetail(m_detail->value->MemberBegin())),
			JSONObjectIterator(detail::JSONMemberIteratorDetail(m_detail->value->MemberEnd())));
	}

	////////////////////////////////
	//
	//	Array
	//

	size_t JSONValue::arrayCount() const
	{
		if (!isArray())
		{
			return 0;
		}

		return m_detail->value->End() - m_detail->value->Begin();
	}

	JSONArrayView JSONValue::arrayView() const
	{
		if (!isArray())
		{
			return JSONArrayView();
		}

		return JSONArrayView(
			JSONArrayIterator(detail::JSONArrayIteratorDetail(m_detail->value->Begin())),
			JSONArrayIterator(detail::JSONArrayIteratorDetail(m_detail->value->End())));
	}

	////////////////////////////////
	//
	//	String
	//

	String JSONValue::getString() const
	{
		if (!isString())
		{
			return String();
		}

		return String(m_detail->value->GetString(), m_detail->value->GetStringLength());
	}

	Optional<String> JSONValue::getOptString() const
	{
		if (!isString())
		{
			return none;
		}

		return Optional<String>(InPlace, m_detail->value->GetString(), m_detail->value->GetStringLength());
	}

	////////////////////////////////
	//
	//	Number
	//

	Optional<int32> JSONValue::getOptInt32() const
	{
		if (isEmpty() || !m_detail->value->IsInt())
		{
			return none;
		}

		return m_detail->value->GetInt();
	}

	Optional<uint32> JSONValue::getOptUint32() const
	{
		if (isEmpty() || !m_detail->value->IsUint())
		{
			return none;
		}

		return m_detail->value->GetUint();
	}

	Optional<int64> JSONValue::getOptInt64() const
	{
		if (isEmpty() || !m_detail->value->IsInt64())
		{
			return none;
		}

		return m_detail->value->GetInt64();
	}

	Optional<uint64> JSONValue::getOptUint64() const
	{
		if (isEmpty() || !m_detail->value->IsUint64())
		{
			return none;
		}

		return m_detail->value->GetUint64();
	}

	Optional<float> JSONValue::getOptFloat() const
	{
		if (isEmpty() || !m_detail->value->IsNumber())
		{
			return none;
		}

		return m_detail->value->GetFloat();
	}

	Optional<double> JSONValue::getOptDouble() const
	{
		if (isEmpty() || !m_detail->value->IsNumber())
		{
			return none;
		}

		return m_detail->value->GetDouble();
	}

	////////////////////////////////
	//
	//	Bool
	//

	Optional<bool> JSONValue::getOptBool() const
	{
		if (!isBool())
		{
			return none;
		}

		return m_detail->value->IsTrue();
	}

	////////////////////////////////
	//
	//	JSONReader
	//

	JSONReader::JSONReader()
		: m_document(std::make_shared<detail::JSONDocumentDetail>())
	{

	}

	JSONReader::JSONReader(const FilePathView path)
		: JSONReader()
	{
		open(path);
	}

	JSONReader::JSONReader(const std::shared_ptr<IReader>& reader)
		: JSONReader()
	{
		open(reader);
	}

	bool JSONReader::open(const FilePathView path)
	{
		if (isOpen())
		{
			close();
		}

		const String text = TextReader(path).readAll();

		rapidjson::GenericStringStream<rapidjson::UTF32<char32>> m_stream(text.c_str());

		constexpr uint32 flags = rapidjson::kParseCommentsFlag
			| rapidjson::kParseTrailingCommasFlag
			| rapidjson::kParseNanAndInfFlag;

		m_document->document.ParseStream<flags>(m_stream);

		if (m_document->document.HasParseError())
		{
			return false;
		}

		m_detail->value.emplace(m_document->document);

		return true;
	}

	bool JSONReader::open(const std::shared_ptr<IReader>& reader)
	{
		if (isOpen())
		{
			close();
		}

		const String text = TextReader(reader).readAll();

		rapidjson::GenericStringStream<rapidjson::UTF32<char32>> m_stream(text.c_str());

		constexpr uint32 flags = rapidjson::kParseCommentsFlag
			| rapidjson::kParseTrailingCommasFlag
			| rapidjson::kParseNanAndInfFlag;

		m_document->document.ParseStream<flags>(m_stream);

		if (m_document->document.HasParseError())
		{
			return false;
		}

		m_detail->value.emplace(m_document->document);

		return true;
	}

	void JSONReader::close()
	{
		m_detail->value.reset();

		m_document->document = rapidjson::GenericDocument<rapidjson::UTF32<char32>>{};
	}

	bool JSONReader::isOpen() const
	{
		return m_detail->value.has_value();
	}

	JSONReader::operator bool() const
	{
		return isOpen();
	}
}
