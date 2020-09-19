/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flink.sql.sink.impala;

import com.dtstack.flink.sql.sink.IStreamSinkGener;
import com.dtstack.flink.sql.sink.impala.table.ImpalaTableInfo;
import com.dtstack.flink.sql.sink.rdb.JDBCOptions;
import com.dtstack.flink.sql.sink.rdb.AbstractRdbSink;
import com.dtstack.flink.sql.table.AbstractTargetTableInfo;

/**
 * Date: 2019/11/11
 * Company: www.dtstack.com
 *
 * @author xiuzhu
 */

public class ImpalaSink extends AbstractRdbSink implements IStreamSinkGener<AbstractRdbSink> {

    private ImpalaTableInfo impalaTableInfo;

    public ImpalaSink() {
        super(null);
    }

    @Override
    public ImpalaOutputFormat getOutputFormat() {
        JDBCOptions jdbcOptions = JDBCOptions.builder()
                .setDbUrl(getImpalaJdbcUrl())
                .setDialect(new ImpalaDialect(getFieldTypes(), primaryKeys))
                .setUsername(userName)
                .setPassword(password)
                .setTableName(tableName)
                .build();

        return ImpalaOutputFormat.impalaBuilder()
                .setOptions(jdbcOptions)
                .setFieldNames(fieldNames)
                .setFlushMaxSize(batchNum)
                .setFlushIntervalMills(batchWaitInterval)
                .setFieldTypes(sqlTypes)
                .setKeyFields(primaryKeys)
                .setPartitionFields(impalaTableInfo.getPartitionFields())
                .setAllReplace(allReplace)
                .setUpdateMode(updateMode)
                .setAuthMech(impalaTableInfo.getAuthMech())
                .setKeytabPath(impalaTableInfo.getKeyTabFilePath())
                .setKrb5confPath(impalaTableInfo.getKrb5FilePath())
                .setPrincipal(impalaTableInfo.getPrincipal())
                .build();
    }

    public String getImpalaJdbcUrl() {
        Integer authMech = impalaTableInfo.getAuthMech();
        String newUrl = dbUrl;
        StringBuffer urlBuffer = new StringBuffer(dbUrl);
        if (authMech == EAuthMech.NoAuthentication.getType()) {
            return newUrl;
        } else if (authMech == EAuthMech.Kerberos.getType()) {
            String krbRealm = impalaTableInfo.getKrbRealm();
            String krbHostFqdn = impalaTableInfo.getKrbHostFQDN();
            String krbServiceName = impalaTableInfo.getKrbServiceName();
            urlBuffer.append(";"
                    .concat("AuthMech=1;")
                    .concat("KrbRealm=").concat(krbRealm).concat(";")
                    .concat("KrbHostFQDN=").concat(krbHostFqdn).concat(";")
                    .concat("KrbServiceName=").concat(krbServiceName).concat(";")
            );
            newUrl = urlBuffer.toString();
        } else if (authMech == EAuthMech.UserName.getType()) {
            urlBuffer.append(";"
                    .concat("AuthMech=3;")
                    .concat("UID=").concat(userName).concat(";")
                    .concat("PWD=;")
                    .concat("UseSasl=0")
            );
            newUrl = urlBuffer.toString();
        } else if (authMech == EAuthMech.NameANDPassword.getType()) {
            urlBuffer.append(";"
                    .concat("AuthMech=3;")
                    .concat("UID=").concat(userName).concat(";")
                    .concat("PWD=").concat(password)
            );
            newUrl = urlBuffer.toString();
        } else {
            throw new IllegalArgumentException("The value of authMech is illegal, Please select 0, 1, 2, 3");
        }
        return newUrl;
    }

    @Override
    public AbstractRdbSink genStreamSink(AbstractTargetTableInfo targetTableInfo) {
        super.genStreamSink(targetTableInfo);
        this.impalaTableInfo = (ImpalaTableInfo) targetTableInfo;
        return this;
    }

}
