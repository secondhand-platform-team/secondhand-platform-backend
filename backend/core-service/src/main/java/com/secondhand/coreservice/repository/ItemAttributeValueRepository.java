package com.secondhand.coreservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.ItemAttributeValue;

@Repository
public interface ItemAttributeValueRepository extends JpaRepository<ItemAttributeValue, String> {
    
    //  Tìm tất cả attribute values của một item
    
    List<ItemAttributeValue> findByItemItemId(String itemId);

   
     //  Tìm attribute value theo item và attribute
     
    Optional<ItemAttributeValue> findByItem_ItemIdAndAttribute_AttributeId(String itemId, String attributeId);

  
    // Xóa toàn bộ attribute values của một item
   
    void deleteByItem_ItemId(String itemId);
}
