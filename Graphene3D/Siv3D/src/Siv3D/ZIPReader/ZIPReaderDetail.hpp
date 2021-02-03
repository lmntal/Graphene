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
# include <Siv3D/ZIPReader.hpp>

# if SIV3D_PLATFORM(WINDOWS)

# include <Siv3D/Windows.hpp>

namespace s3d
{
	class ZIPResourceHolder
	{
	private:

		int64 m_size = 0;

		const void* m_pResource = nullptr;

	public:

		ZIPResourceHolder() = default;

		ZIPResourceHolder(const FilePathView path)
		{
			HMODULE hModule = ::GetModuleHandleW(nullptr);

			const std::wstring pathW = Unicode::ToWString(path);

			if (HRSRC hrs = ::FindResourceW(hModule, &pathW[1], L"FILE"))
			{
				if (HGLOBAL resource = ::LoadResource(hModule, hrs))
				{
					m_pResource = ::LockResource(resource);

					m_size = ::SizeofResource(hModule, hrs);
				}
			}
		}

		~ZIPResourceHolder()
		{
			m_pResource = nullptr;

			m_size = 0;
		}

		const void* data() const
		{
			return m_pResource;
		}

		int64 size() const
		{
			return m_size;
		}
	};
}

# endif

namespace s3d
{
	class ZIPReader::ZIPReaderDetail
	{
	private:

		void* m_reader = nullptr;

		FilePath m_archiveFileFullPath;

		Array<FilePath> m_paths;

	# if SIV3D_PLATFORM(WINDOWS)

		ZIPResourceHolder m_resource;

	# endif

	public:

		ZIPReaderDetail();

		~ZIPReaderDetail();

		bool open(FilePathView path);

		void close();

		[[nodiscard]] bool isOpen() const;

		[[nodiscard]] const Array<FilePath>& enumPaths() const;

		bool extractAll(FilePathView targetDirectory) const;

		bool extract(StringView pattern, FilePathView targetDirectory) const;

		[[nodiscard]] ByteArray extractToMemory(FilePathView filePath) const;
	};
}
