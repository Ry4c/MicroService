package com.demo.orderservice.service;

import com.demo.orderservice.dto.InventoryResponse;
import com.demo.orderservice.dto.OderLineItemDto;
import com.demo.orderservice.dto.OrderRequest;
import com.demo.orderservice.model.OderLineItem;
import com.demo.orderservice.model.Order;
import com.demo.orderservice.repository.OrderRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepo orderRepo;
    private final WebClient.Builder webClientBuilder;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OderLineItem> listOderLineItems = orderRequest.getOderLineItemDtoList().stream()
                .map(this::mapToDto)
                .toList();
        order.setOderLineItemList(listOderLineItems);

        List<String> skuCodeList = listOderLineItems.stream().map(OderLineItem::getSkuCode).toList();

        InventoryResponse[] result = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",skuCodeList).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .blockOptional().orElseThrow(()-> new IllegalArgumentException("product is not in stock"));

        boolean allMatch = Arrays.stream(result).allMatch(InventoryResponse::isInStock);
        if (allMatch){
            orderRepo.save(order);
            return "Oder Placed Successfully";
        }
        return "Something's Out Of Stock";
    }

    private OderLineItem mapToDto(OderLineItemDto oderLineItemDto) {
        OderLineItem oderLineItem = new OderLineItem();
        oderLineItem.setPrice(oderLineItemDto.getPrice());
        oderLineItem.setQuantity(oderLineItemDto.getQuantity());
        oderLineItem.setSkuCode(oderLineItemDto.getSkuCode());
        return oderLineItem;
    }
}
