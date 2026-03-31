package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WppConnectionRepository extends JpaRepository<WppConnection, Long> {

    Optional<WppConnection> findByPhoneNumberId(String phoneNumberId);

    Optional<WppConnection> findByWabaId(String wabaId);


    Optional<WppConnection> findFirstByTenantIdAndStatusOrderByIdDesc(
            String tenantId, WppConnection.WppConnectionStatus status);

    List<WppConnection> findAllByTenantId(String tenantId);


    @Modifying
    @Query("UPDATE WppConnection c SET c.status = com.mipyme.whatsapp.model.WppConnection.WppConnectionStatus.DISCONNECTED "
         + "WHERE c.tenantId = :tenantId AND c.id <> :keepId")
    void deactivateOtherConnections(@Param("tenantId") String tenantId, @Param("keepId") Long keepId);
}
