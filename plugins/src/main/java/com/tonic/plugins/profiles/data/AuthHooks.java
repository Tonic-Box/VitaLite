package com.tonic.plugins.profiles.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthHooks {
    public int setLoginIndexGarbageValue;
    public String setLoginIndexMethodName;
    public String setLoginIndexClassName;
    public String jxSessionFieldName;
    public String jxSessionClassName;
    public String jxAccountIdFieldName;
    public String jxAccountIdClassName;
    public String jxDisplayNameFieldName;
    public String jxDisplayNameClassName;
    public String jxAccountCheckFieldName;
    public String jxAccountCheckClassName;
    public String jxLegacyValueFieldName;
    public String jxLegacyValueClassName;
}
