package com.mugunghwa.goodquestion.learning.reward.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findAllByOrderByDisplayOrderAsc();
}
