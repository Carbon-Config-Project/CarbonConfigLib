package carbonconfiglib.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import carbonconfiglib.api.ConfigType;
import carbonconfiglib.api.IConfigProxy;
import carbonconfiglib.api.ILogger;
import carbonconfiglib.api.SimpleConfigProxy;
import carbonconfiglib.utils.AutomationType;
import carbonconfiglib.utils.MultilinePolicy;

/**
 * Copyright 2023 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ConfigSettings {
	AutomationType[] auto;
	MultilinePolicy policy;
	ConfigType type;
	ILogger logger;
	IConfigProxy proxy;
	String subFolder;
	
	private ConfigSettings() {}
	
	public static ConfigSettings of() {
		return new ConfigSettings();
	}
	
	public static ConfigSettings withPath(Path baseFolder) {
		return new ConfigSettings().withBaseFolder(baseFolder);
	}
	
	public static ConfigSettings withFolderProxy(IConfigProxy proxy) {
		return new ConfigSettings().withProxy(proxy);
	}
	
	public static ConfigSettings withConfigType(ConfigType type) {
		return new ConfigSettings().withType(type);
	}
	
	public static ConfigSettings withoutSettings() {
		return new ConfigSettings().noAutomations();
	}
	
	public static ConfigSettings withSettings(AutomationType... type) {
		return new ConfigSettings().withAutomations(type);
	}
	
	public static ConfigSettings withLog(ILogger logger) {
		return new ConfigSettings().withLogger(logger);
	}
	
	public static ConfigSettings withFolder(String subFolder) {
		return new ConfigSettings().withSubFolder(subFolder);
	}
	
	public static ConfigSettings withLinePolicy(MultilinePolicy policy) {
		return new ConfigSettings().withMultiline(policy);
	}
	
	public ConfigSettings withBaseFolder(Path baseFolder) {
		return withProxy(new SimpleConfigProxy(baseFolder));
	}
	
	public ConfigSettings withProxy(IConfigProxy proxy) {
		if(this.proxy == null) this.proxy = proxy;
		return this;
	}
	
	public ConfigSettings noAutomations() {
		if(auto == null) this.auto = new AutomationType[0];
		return this;
	}
	
	public ConfigSettings withAutomations(AutomationType... auto) {
		if(this.auto == null) this.auto = auto;
		return this;
	}
	
	public ConfigSettings withMultiline(MultilinePolicy policy) {
		if(this.policy == null) this.policy = policy;
		return this;
	}
	
	public ConfigSettings withType(ConfigType type) {
		if(this.type == null) this.type = type;
		return this;
	}
	
	public ConfigSettings withLogger(ILogger logger) {
		if(this.logger == null) this.logger = logger;
		return this;
	}
	
	public ConfigSettings withSubFolder(String subFolder) {
		if(this.subFolder == null) this.subFolder = subFolder;
		return this;
	}
	
	public IConfigProxy getProxy() {
		return proxy;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	public String getSubFolder() {
		return subFolder;
	}
	
	public ConfigType getType() {
		return type;
	}
	
	public EnumSet<AutomationType> getAutomationType() {
		return EnumSet.copyOf(Arrays.asList(auto));
	}
	
	public MultilinePolicy getMultilinePolicy() {
		return policy;
	}
}
