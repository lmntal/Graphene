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

# include <Siv3D/Dialog.hpp>
# include <Siv3D/FileSystem.hpp>
# import  <Foundation/Foundation.h>
# import  <AppKit/AppKit.h>

namespace s3d
{
	namespace detail
	{
		static Array<std::string> ConvertFilters(const Array<FileFilter>& filters)
		{
			// All files
			if (filters.includes_if([](const FileFilter& f){ return f.patterns.isEmpty(); }))
			{
				return {};
			}
			
			Array<std::string> filtersUTF8;
			
			for (const auto& filter: filters)
			{
				for (const auto& pattern : filter.patterns)
				{
					filtersUTF8 << pattern.toUTF8();
				}
			}
			
			return filtersUTF8;
		}
		
		static bool SetDefaultPath(NSSavePanel* dialog, const FilePath& defaultPath)
		{
			if (!defaultPath)
			{
				return true;
			}
			
			NSString* defaultPathStr = [NSString stringWithUTF8String: defaultPath.toUTF8().c_str()];
			NSURL* url = [NSURL fileURLWithPath:defaultPathStr isDirectory:YES];
			[dialog setDirectoryURL:url];
			
			return true;
		}
	}
	
	namespace Dialog
	{
		Optional<FilePath> OpenFile(const Array<FileFilter>& filters, const FilePath& defaultPath, const String&)
		{
			NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
			NSWindow* keyWindow = [[NSApplication sharedApplication] keyWindow];
			NSOpenPanel* dialog = [NSOpenPanel openPanel];
			[dialog setAllowsMultipleSelection:NO];
			[dialog setFloatingPanel:YES];
			[dialog setCanChooseDirectories:NO];
			[dialog setCanChooseFiles:YES];
			
			if (const auto filtersUTF8 = detail::ConvertFilters(filters); filtersUTF8)
			{
				NSMutableArray* filterList = [[NSMutableArray alloc] init];
				
				for (const auto& filter : filtersUTF8)
				{
					NSString* filterType = [NSString stringWithUTF8String: filter.c_str()];
					[filterList addObject:filterType];
				}
				
				NSArray* result = [NSArray arrayWithArray:filterList];
				[dialog setAllowedFileTypes:result];
			}
			
			if (!detail::SetDefaultPath(dialog, defaultPath))
			{
				return none;
			}
			
			Optional<FilePath> result;
			
			if ([dialog runModal] == NSModalResponseOK)
			{
				NSURL* url = [dialog URL];
				result = Unicode::FromUTF8([[url path] UTF8String]);
			}
			
			[pool release];
			[keyWindow makeKeyAndOrderFront:nil];
			
			return result;
		}
		
		Array<FilePath> OpenFiles(const Array<FileFilter>& filters, const FilePath& defaultPath, const String&)
		{
			NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
			NSWindow* keyWindow = [[NSApplication sharedApplication] keyWindow];
			NSOpenPanel* dialog = [NSOpenPanel openPanel];
			[dialog setAllowsMultipleSelection:YES];
			[dialog setFloatingPanel:YES];
			[dialog setCanChooseDirectories:NO];
			[dialog setCanChooseFiles:YES];
			
			if (const auto filtersUTF8 = detail::ConvertFilters(filters); filtersUTF8)
			{
				NSMutableArray* filterList = [[NSMutableArray alloc] init];
				
				for (const auto& filter : filtersUTF8)
				{
					NSString* filterType = [NSString stringWithUTF8String: filter.c_str()];
					[filterList addObject:filterType];
				}
				
				NSArray* result = [NSArray arrayWithArray:filterList];
				[dialog setAllowedFileTypes:result];
			}
			
			if (!detail::SetDefaultPath(dialog, defaultPath))
			{
				return{};
			}
			
			Array<FilePath> result;
			
			if ([dialog runModal] == NSModalResponseOK)
			{
				NSArray* urls = [dialog URLs];
				
				if ([urls count] == 0)
				{
					[pool release];
					return{};
				}
				
				for (NSURL *url in urls)
				{
					NSString* path = [url path];
					result << Unicode::FromUTF8([path UTF8String]);
				}
			}
			
			[pool release];
			[keyWindow makeKeyAndOrderFront:nil];
			
			return result;
		}
		
		Optional<FilePath> SaveFile(const Array<FileFilter>& filters, const FilePath& defaultPath, const String&)
		{
			NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
			
			NSSavePanel* dialog = [NSSavePanel savePanel];
			[dialog setExtensionHidden:NO];
			[dialog setFloatingPanel:YES];
			
			if (const auto filtersUTF8 = detail::ConvertFilters(filters); filtersUTF8)
			{
				NSMutableArray* filterList = [[NSMutableArray alloc] init];
				
				for (const auto& filter : filtersUTF8)
				{
					NSString* filterType = [NSString stringWithUTF8String: filter.c_str()];
					[filterList addObject:filterType];
				}
				
				NSArray* result = [NSArray arrayWithArray:filterList];
				[dialog setAllowedFileTypes:result];
			}
			
			if (!detail::SetDefaultPath(dialog, defaultPath))
			{
				return none;
			}
			
			Optional<FilePath> result;
			
			if ([dialog runModal] == NSModalResponseOK)
			{
				NSURL* url = [dialog URL];
				result = Unicode::FromUTF8([[url path] UTF8String]);
			}
			
			[pool release];
			
			return result;
		}
		
		Optional<FilePath> SelectFolder(const FilePath& defaultPath, const String&)
		{
			NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
			NSWindow* keyWindow = [[NSApplication sharedApplication] keyWindow];
			NSOpenPanel* dialog = [NSOpenPanel openPanel];
			[dialog setAllowsMultipleSelection:NO];
			[dialog setFloatingPanel:YES];
			[dialog setCanChooseDirectories:YES];
			[dialog setCanCreateDirectories:YES];
			[dialog setCanChooseFiles:NO];
			
			if (!detail::SetDefaultPath(dialog, defaultPath))
			{
				return none;
			}
			
			Optional<FilePath> result;
			
			if ([dialog runModal] == NSModalResponseOK)
			{
				NSURL* url = [dialog URL];
				result = Unicode::FromUTF8([[url path] UTF8String]);
			}
			
			[pool release];
			[keyWindow makeKeyAndOrderFront:nil];
			
			return result;
		}
	}
}
