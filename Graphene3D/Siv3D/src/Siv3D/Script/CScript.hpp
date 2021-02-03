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
# include <AssetHandleManager/AssetHandleManager.hpp>
# include <Siv3D/HashTable.hpp>
# include "ScriptData.hpp"
# include "IScript.hpp"

namespace s3d
{
	class CScript : public ISiv3DScript
	{
	private:

		AngelScript::asIScriptEngine* m_engine = nullptr;

		AssetHandleManager<ScriptID, ScriptData> m_scripts{ U"Script" };

		bool m_shutDown = true;
		
		Array<String> m_messageArray;

	public:

		CScript();

		~CScript() override;

		bool init() override;

		void shutdown() override;

		ScriptID createFromCode(const String& code, int32 compileOption) override;

		ScriptID createFromFile(const FilePath& path, int32 compileOption) override;

		void release(ScriptID handleID) override;

		AngelScript::asIScriptFunction* getFunction(ScriptID handleID, const String& decl) override;

		std::shared_ptr<ScriptModuleData> getModuleData(ScriptID handleID) override;

		bool compiled(ScriptID handleID) override;

		void setSystemUpdateCallback(ScriptID handleID, const std::function<bool(void)>& callback) override;

		bool reload(ScriptID handleID, int32 compileOption) override;

		const FilePath& path(ScriptID handleID) override;

		Array<String> retrieveMessagesInternal() override;

		const Array<String>& retrieveMessages(ScriptID handleID) override;

		const std::function<bool(void)>& getSystemUpdateCallback(uint64 scriptID) override;

		AngelScript::asIScriptEngine* getEngine() override;
	};
}
