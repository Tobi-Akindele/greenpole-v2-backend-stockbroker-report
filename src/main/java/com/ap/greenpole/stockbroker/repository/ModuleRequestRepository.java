package com.ap.greenpole.stockbroker.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ap.greenpole.stockbroker.model.ModuleRequest;

@Repository
public interface ModuleRequestRepository extends JpaRepository<ModuleRequest, Long>{

	public List<ModuleRequest> findAllModuleRequestByModulesAndActionRequired(String modules, String actionRequired);
	public Page<ModuleRequest> findAllModuleRequestByModulesAndActionRequired(String modules, Pageable pageable, String actionRequired);
	public ModuleRequest findModuleRequestByRequestIdAndModulesAndActionRequired(Long requestId, String modules, String actionRequired);
	public ModuleRequest findFirstModuleRequestByResourceIdAndActionRequiredAndModulesOrderByRequestIdDesc(Long resourceId,
			String actionRequired, String modules);
	
}
