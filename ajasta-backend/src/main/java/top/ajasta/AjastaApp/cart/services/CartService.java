package top.ajasta.AjastaApp.cart.services;

import top.ajasta.AjastaApp.cart.dtos.CartDTO;
import top.ajasta.AjastaApp.response.Response;

public interface CartService {

    Response<?> addItemToCart(CartDTO cartDTO);
    Response<?> incrementItem(Long menuId);
    Response<?> decrementItem(Long menuId);
    Response<?> removeItem(Long cartItemId);
    Response<CartDTO> getShoppingCart();
    Response<?> clearShoppingCart();
}
