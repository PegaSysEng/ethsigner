/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner;

import static tech.pegasys.ethsigner.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;

import java.io.File;
import java.util.List;

import picocli.CommandLine;

// Allows to obtain config file by PicoCLI using two pass approach.
@CommandLine.Command(mixinStandardHelpOptions = true)
class ConfigFileCommand {
  @CommandLine.Option(names = CONFIG_FILE_OPTION_NAME, description = "...")
  File configPath = null;

  @SuppressWarnings("UnusedVariable")
  @CommandLine.Unmatched
  List<String> unmatched;
}
