package com.shop.controller;
import com.shop.entity.Cart;
import com.shop.entity.LineItem;
import com.shop.entity.Product;
import com.shop.service.CartService;
import com.shop.service.LineItemService;
import com.shop.service.ProductService;
import com.shop.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import com.shop.dto.CartDTO;
import com.shop.dto.LineItemDTO;
import java.util.stream.Collectors;

@RequestMapping("/api/shop/cart")
@RestController
public class CartController2 {

    @Autowired
    private CartService cartService;
    @Autowired
    private LineItemService lineItemService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CustomerService customerService;

    @GetMapping("/viewcart")
    public ResponseEntity<CartDTO> viewCart(@RequestParam Long customerId) {
        Cart cart = cartService.getCartByCustomer_Id(customerId);
        if (cart != null) {
            return ResponseEntity.ok(convertToDTO(cart));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CartDTO> addToCart(@RequestParam Long customerId, @RequestBody Product productRequest) {
        Cart existingCart = cartService.getCartByCustomer_Id(customerId);

        if (existingCart == null) {
            Cart newCart = new Cart();
            newCart.setCustomer(customerService.getCustomerById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found")));
            newCart.setLineItemList(new ArrayList<>());

            LineItem newLineItem = createLineItem(productRequest, newCart);
            newCart.getLineItemList().add(newLineItem);

            Cart savedCart = cartService.save(newCart);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedCart));
        } else {
            LineItem lineItemExists = existingCart.getLineItemList().stream()
                    .filter(li -> li.getProduct().getId().equals(productRequest.getId()))
                    .findFirst()
                    .orElse(null);

            if (lineItemExists != null) {
                lineItemExists.setQuantity(lineItemExists.getQuantity() + 1);
                lineItemExists.setItemTotal(lineItemExists.getUnitPrice() * lineItemExists.getQuantity());
                lineItemService.save(lineItemExists);
            } else {
                LineItem newLineItem = createLineItem(productRequest, existingCart);
                existingCart.getLineItemList().add(newLineItem);
                lineItemService.save(newLineItem);
            }

            Cart updatedCart = cartService.save(existingCart);
            return ResponseEntity.ok(convertToDTO(updatedCart));
        }
    }

    private LineItem createLineItem(Product product, Cart cart) {
        LineItem lineItem = new LineItem();
        lineItem.setProduct(product);
        lineItem.setUnitPrice(product.getPrice());
        lineItem.setQuantity(1);
        lineItem.setItemTotal(product.getPrice());
        lineItem.setCart(cart);
        return lineItem;
    }

    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getCartId());
        dto.setLineItems(cart.getLineItemList().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        return dto;
    }

    private LineItemDTO convertToDTO(LineItem lineItem) {
        LineItemDTO dto = new LineItemDTO();
        dto.setId(lineItem.getLineItemId());
        dto.setProductId(lineItem.getProduct().getId());
        dto.setQuantity(lineItem.getQuantity());
        dto.setItemTotal(lineItem.getItemTotal());
        return dto;
    }
}





















