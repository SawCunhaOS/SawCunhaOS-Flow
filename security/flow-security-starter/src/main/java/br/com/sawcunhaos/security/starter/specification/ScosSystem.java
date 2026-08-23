package br.com.sawcunhaos.security.starter.specification;

import java.util.List;

public interface ScosSystem {

    String getSystemName();
    String getSystemCode();
    String getSystemDescription();
    List<ScosPermission> getPermissions();

}
