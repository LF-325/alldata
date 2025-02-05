/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.plugin.unstructuredstorage.Compress;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.format.FileFormat;
import com.qlangtech.tis.plugin.datax.meta.MetaDataWriter;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.writer.ftpwriter.FtpWriter
 **/
@Public
public class DataXFtpWriter extends DataxWriter implements IDataXBatchPost {
    public static final String KEY_FTP_SERVER_LINK = "linker";

    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String linker;

    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.absolute_path})
    public String path;

    /**
     * 写入数据过程中会在ftp目录中写一份source的元数据
     */
    @FormField(ordinal = 7, validate = {Validator.require})
    public MetaDataWriter writeMetaData;

    //    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
//    public String fileName;
    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {Validator.require})
    public String writeMode;
    //    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {})
//    public String fieldDelimiter;
    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {Validator.require})
    public String compress;

    @FormField(ordinal = 11, type = FormFieldType.ENUM, validate = {})
    public String encoding;
    @FormField(ordinal = 12, type = FormFieldType.INPUTTEXT, validate = {})
    public String nullFormat;
    @FormField(ordinal = 13, type = FormFieldType.INPUTTEXT, validate = {})
    public String dateFormat;
    @FormField(ordinal = 14, validate = {Validator.require})
    public FileFormat fileFormat;

    @Override
    public ExecutePhaseRange getPhaseRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.FullDump);
    }

    @Override
    public IRemoteTaskTrigger createPreExecuteTask(IExecChainContext execContext, ISelectedTab tab) {
        return writeMetaData.createMetaDataWriteTask(this, execContext, tab);
    }

    @Override
    public IRemoteTaskTrigger createPostTask(IExecChainContext execContext, ISelectedTab tab, DataXCfgGenerator.GenerateCfgs cfgFileNames) {
        return null;
    }

    public static List<Option> supportCompress() {
        return Arrays.stream(Compress.values()).filter((c) -> c.supportWriter())
                .map((c) -> new Option(c.name(), c.token)).collect(Collectors.toList());
    }


    @FormField(ordinal = 17, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXFtpWriter.class, "DataXFtpWriter-tpl.json");
    }


    @Override
    public String getTemplate() {
        return this.template;
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        DataXFtpWriterContext writerContext = new DataXFtpWriterContext(this, tableMap.get());
        return writerContext;
    }


    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxWriterDescriptor {
        public DefaultDescriptor() {
            super();
            registerSelectOptions(KEY_FTP_SERVER_LINK, () -> ParamsConfig.getItems(FTPServer.FTP_SERVER));
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.FTP;
        }

        @Override
        public boolean isRdbms() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return DataXFtpReader.DATAX_NAME;
        }
    }
}
