package top.ajasta.AjastaApp.config;

import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.auth_users.repository.UserRepository;
import top.ajasta.AjastaApp.cart.entity.Cart;
import top.ajasta.AjastaApp.cart.entity.CartItem;
import top.ajasta.AjastaApp.cart.repository.CartRepository;
import top.ajasta.AjastaApp.category.entity.Category;
import top.ajasta.AjastaApp.category.repository.CategoryRepository;
import top.ajasta.AjastaApp.enums.OrderStatus;
import top.ajasta.AjastaApp.enums.PaymentGateway;
import top.ajasta.AjastaApp.enums.PaymentStatus;
import top.ajasta.AjastaApp.menu.entity.Menu;
import top.ajasta.AjastaApp.menu.repository.MenuRepository;
import top.ajasta.AjastaApp.order.entity.Order;
import top.ajasta.AjastaApp.order.entity.OrderItem;
import top.ajasta.AjastaApp.order.repository.OrderRepository;
import top.ajasta.AjastaApp.payment.entity.Payment;
import top.ajasta.AjastaApp.payment.repository.PaymentRepository;
import top.ajasta.AjastaApp.review.entity.Review;
import top.ajasta.AjastaApp.review.repository.ReviewRepository;
import top.ajasta.AjastaApp.role.entity.Role;
import top.ajasta.AjastaApp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            
            // Initialize Roles
            initializeRoles();
            
            // Initialize Users
            initializeUsers();
            
            // Skipping initialization for Categories, Menu Items, Carts, Orders, Payments, and Reviews as these features are removed
            
            log.info("Database initialization completed successfully!");
        } else {
            log.info("Database already contains data. Skipping initialization.");
        }
    }

    private void initializeRoles() {
        List<Role> roles = Arrays.asList(
            Role.builder().name("ADMIN").build(),
            Role.builder().name("USER").build(),
            Role.builder().name("RESOURCE_MANAGER").build()
        );
        roleRepository.saveAll(roles);
        log.info("Initialized {} roles", roles.size());
    }

    private void initializeCategories() {
        List<Category> categories = Arrays.asList(
            Category.builder()
                .name("Appetizers")
                .description("Start your meal with our delicious appetizers")
                .build(),
            Category.builder()
                .name("Main Courses")
                .description("Hearty and satisfying main dishes")
                .build(),
            Category.builder()
                .name("Desserts")
                .description("Sweet treats to end your meal perfectly")
                .build(),
            Category.builder()
                .name("Beverages")
                .description("Refreshing drinks and hot beverages")
                .build(),
            Category.builder()
                .name("Salads")
                .description("Fresh and healthy salad options")
                .build(),
            Category.builder()
                .name("Pizza")
                .description("Wood-fired pizzas with premium toppings")
                .build(),
            Category.builder()
                .name("Pasta")
                .description("Italian pasta dishes made fresh daily")
                .build(),
            Category.builder()
                .name("Burgers")
                .description("Gourmet burgers with premium ingredients")
                .build()
        );
        categoryRepository.saveAll(categories);
        log.info("Initialized {} categories", categories.size());
    }

    private void initializeMenuItems() {
        List<Category> categories = categoryRepository.findAll();
        List<Menu> menuItems = new ArrayList<>();

        // Appetizers
        Category appetizers = categories.stream().filter(c -> c.getName().equals("Appetizers")).findFirst().orElse(null);
        if (appetizers != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Buffalo Wings")
                    .description("Crispy chicken wings tossed in spicy buffalo sauce")
                    .price(new BigDecimal("12.99"))
                    .imageUrl("https://images.unsplash.com/photo-1567620832903-9fc6debc209f")
                    .category(appetizers)
                    .build(),
                Menu.builder()
                    .name("Mozzarella Sticks")
                    .description("Golden fried mozzarella with marinara sauce")
                    .price(new BigDecimal("8.99"))
                    .imageUrl("https://images.unsplash.com/photo-1548940740-204726a19be3")
                    .category(appetizers)
                    .build(),
                Menu.builder()
                    .name("Nachos Supreme")
                    .description("Loaded nachos with cheese, jalapeños, and sour cream")
                    .price(new BigDecimal("11.99"))
                    .imageUrl("https://images.unsplash.com/photo-1513456852971-30c0b8199d4d")
                    .category(appetizers)
                    .build()
            ));
        }

        // Main Courses
        Category mainCourses = categories.stream().filter(c -> c.getName().equals("Main Courses")).findFirst().orElse(null);
        if (mainCourses != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Grilled Salmon")
                    .description("Fresh Atlantic salmon with herbs and lemon")
                    .price(new BigDecimal("24.99"))
                    .imageUrl("https://images.unsplash.com/photo-1467003909585-2f8a72700288")
                    .category(mainCourses)
                    .build(),
                Menu.builder()
                    .name("Ribeye Steak")
                    .description("12oz prime ribeye cooked to perfection")
                    .price(new BigDecimal("32.99"))
                    .imageUrl("https://images.unsplash.com/photo-1558030006-450675393462")
                    .category(mainCourses)
                    .build(),
                Menu.builder()
                    .name("Chicken Parmesan")
                    .description("Breaded chicken breast with marinara and mozzarella")
                    .price(new BigDecimal("19.99"))
                    .imageUrl("https://images.unsplash.com/photo-1632778149955-e80f8ceca2e8")
                    .category(mainCourses)
                    .build()
            ));
        }

        // Pizza
        Category pizza = categories.stream().filter(c -> c.getName().equals("Pizza")).findFirst().orElse(null);
        if (pizza != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Margherita Pizza")
                    .description("Classic pizza with tomato, mozzarella, and basil")
                    .price(new BigDecimal("16.99"))
                    .imageUrl("https://images.unsplash.com/photo-1604382354936-07c5d9983bd3")
                    .category(pizza)
                    .build(),
                Menu.builder()
                    .name("Pepperoni Pizza")
                    .description("Traditional pepperoni with mozzarella cheese")
                    .price(new BigDecimal("18.99"))
                    .imageUrl("https://images.unsplash.com/photo-1565299624946-b28f40a0ca4b")
                    .category(pizza)
                    .build(),
                Menu.builder()
                    .name("Supreme Pizza")
                    .description("Loaded with pepperoni, sausage, peppers, and mushrooms")
                    .price(new BigDecimal("22.99"))
                    .imageUrl("https://images.unsplash.com/photo-1513104890138-7c749659a591")
                    .category(pizza)
                    .build()
            ));
        }

        // Burgers
        Category burgers = categories.stream().filter(c -> c.getName().equals("Burgers")).findFirst().orElse(null);
        if (burgers != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Classic Cheeseburger")
                    .description("Beef patty with cheddar, lettuce, tomato, and pickles")
                    .price(new BigDecimal("14.99"))
                    .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd")
                    .category(burgers)
                    .build(),
                Menu.builder()
                    .name("BBQ Bacon Burger")
                    .description("Beef patty with BBQ sauce, bacon, and onion rings")
                    .price(new BigDecimal("17.99"))
                    .imageUrl("https://images.unsplash.com/photo-1553979459-d2229ba7433a")
                    .category(burgers)
                    .build()
            ));
        }

        // Pasta
        Category pasta = categories.stream().filter(c -> c.getName().equals("Pasta")).findFirst().orElse(null);
        if (pasta != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Spaghetti Carbonara")
                    .description("Creamy pasta with bacon, eggs, and parmesan")
                    .price(new BigDecimal("16.99"))
                    .imageUrl("https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5")
                    .category(pasta)
                    .build(),
                Menu.builder()
                    .name("Fettuccine Alfredo")
                    .description("Rich and creamy white sauce with fettuccine pasta")
                    .price(new BigDecimal("15.99"))
                    .imageUrl("https://images.unsplash.com/photo-1645112411341-6c4fd023714a")
                    .category(pasta)
                    .build()
            ));
        }

        // Salads
        Category salads = categories.stream().filter(c -> c.getName().equals("Salads")).findFirst().orElse(null);
        if (salads != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Caesar Salad")
                    .description("Romaine lettuce with caesar dressing and croutons")
                    .price(new BigDecimal("10.99"))
                    .imageUrl("https://images.unsplash.com/photo-1546793665-c74683f339c1")
                    .category(salads)
                    .build(),
                Menu.builder()
                    .name("Greek Salad")
                    .description("Mixed greens with feta, olives, and greek dressing")
                    .price(new BigDecimal("12.99"))
                    .imageUrl("https://images.unsplash.com/photo-1540420773420-3366772f4999")
                    .category(salads)
                    .build()
            ));
        }

        // Desserts
        Category desserts = categories.stream().filter(c -> c.getName().equals("Desserts")).findFirst().orElse(null);
        if (desserts != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Chocolate Cake")
                    .description("Rich chocolate cake with chocolate frosting")
                    .price(new BigDecimal("7.99"))
                    .imageUrl("https://images.unsplash.com/photo-1578985545062-69928b1d9587")
                    .category(desserts)
                    .build(),
                Menu.builder()
                    .name("Cheesecake")
                    .description("New York style cheesecake with berry compote")
                    .price(new BigDecimal("8.99"))
                    .imageUrl("https://images.unsplash.com/photo-1567306301408-9b74779a11af")
                    .category(desserts)
                    .build(),
                Menu.builder()
                    .name("Ice Cream Sundae")
                    .description("Vanilla ice cream with chocolate sauce and whipped cream")
                    .price(new BigDecimal("6.99"))
                    .imageUrl("https://images.unsplash.com/photo-1563805042-7684c019e1cb")
                    .category(desserts)
                    .build()
            ));
        }

        // Beverages
        Category beverages = categories.stream().filter(c -> c.getName().equals("Beverages")).findFirst().orElse(null);
        if (beverages != null) {
            menuItems.addAll(Arrays.asList(
                Menu.builder()
                    .name("Fresh Lemonade")
                    .description("Freshly squeezed lemon juice with a touch of mint")
                    .price(new BigDecimal("3.99"))
                    .imageUrl("https://images.unsplash.com/photo-1621263764928-df1444c5e859")
                    .category(beverages)
                    .build(),
                Menu.builder()
                    .name("Iced Coffee")
                    .description("Cold brew coffee served over ice")
                    .price(new BigDecimal("4.99"))
                    .imageUrl("https://images.unsplash.com/photo-1517701550927-30cf4ba1dfd5")
                    .category(beverages)
                    .build(),
                Menu.builder()
                    .name("Craft Soda")
                    .description("Artisanal sodas in various flavors")
                    .price(new BigDecimal("2.99"))
                    .imageUrl("https://images.unsplash.com/photo-1581636625402-29b2a704ef13")
                    .category(beverages)
                    .build()
            ));
        }

        menuRepository.saveAll(menuItems);
        log.info("Initialized {} menu items", menuItems.size());
    }

    private void initializeUsers() {
        List<Role> roles = roleRepository.findAll();
        Role userRole = roles.stream().filter(r -> r.getName().equals("USER")).findFirst().orElse(null);
        Role adminRole = roles.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().orElse(null);
        Role managerRole = roles.stream().filter(r -> r.getName().equals("RESOURCE_MANAGER")).findFirst().orElse(null);

        List<User> users = Arrays.asList(
            User.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+1234567890")
                .address("123 Main St, New York, NY 10001")
                .isActive(true)
                .roles(Arrays.asList(userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            User.builder()
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+1234567891")
                .address("456 Oak Ave, Los Angeles, CA 90210")
                .isActive(true)
                .roles(Arrays.asList(userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            User.builder()
                .name("Admin User")
                .email("admin@ajastaapp.com")
                .password(passwordEncoder.encode("admin123"))
                .phoneNumber("+1234567892")
                .address("789 Admin Blvd, Chicago, IL 60601")
                .isActive(true)
                .roles(Arrays.asList(adminRole, userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            User.builder()
                .name("Restaurant Manager")
                .email("manager@ajastaapp.com")
                .password(passwordEncoder.encode("manager123"))
                .phoneNumber("+1234567893")
                .address("321 Restaurant St, Miami, FL 33101")
                .isActive(true)
                .roles(Arrays.asList(managerRole, userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            User.builder()
                .name("Alice Johnson")
                .email("alice.johnson@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+1234567894")
                .address("654 Pine St, Seattle, WA 98101")
                .isActive(true)
                .roles(Arrays.asList(userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            User.builder()
                .name("Bob Wilson")
                .email("bob.wilson@example.com")
                .password(passwordEncoder.encode("password123"))
                .phoneNumber("+1234567895")
                .address("987 Elm St, Boston, MA 02101")
                .isActive(true)
                .roles(Arrays.asList(userRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );

        userRepository.saveAll(users);
        log.info("Initialized {} users", users.size());
    }

    private void initializeCarts() {
        List<User> users = userRepository.findAll();
        List<Menu> menuItems = menuRepository.findAll();
        List<Cart> carts = new ArrayList<>();

        for (User user : users) {
            Cart cart = Cart.builder()
                .user(user)
                .cartItems(new ArrayList<>())
                .build();
            
            // Add some sample cart items for regular users (not admin/manager)
            if (user.getRoles().stream().noneMatch(role -> 
                role.getName().equals("ADMIN") || role.getName().equals("RESOURCE_MANAGER"))) {
                
                List<CartItem> cartItems = new ArrayList<>();
                
                // Add 2-3 random items to each user's cart
                for (int i = 0; i < Math.min(3, menuItems.size()); i++) {
                    Menu randomMenu = menuItems.get(i % menuItems.size());
                    CartItem cartItem = CartItem.builder()
                        .cart(cart)
                        .menu(randomMenu)
                        .quantity(1 + (i % 3)) // Quantity between 1-3
                        .build();
                    cartItems.add(cartItem);
                }
                
                cart.setCartItems(cartItems);
            }
            
            carts.add(cart);
        }

        cartRepository.saveAll(carts);
        log.info("Initialized {} carts with cart items", carts.size());
    }

    private void initializeOrders() {
        List<User> users = userRepository.findAll();
        List<Menu> menuItems = menuRepository.findAll();
        List<Order> orders = new ArrayList<>();

        // Create sample orders for users (excluding admin and manager)
        for (User user : users) {
            if (user.getRoles().stream().noneMatch(role -> 
                role.getName().equals("ADMIN") || role.getName().equals("RESOURCE_MANAGER"))) {
                
                // Create 2-3 orders per user
                for (int orderCount = 0; orderCount < 3; orderCount++) {
                    Order order = Order.builder()
                        .user(user)
                        .orderStatus(OrderStatus.values()[orderCount % OrderStatus.values().length])
                        .totalAmount(BigDecimal.ZERO)
                        .orderItems(new ArrayList<>())
                        .orderDate(LocalDateTime.now().minusDays(orderCount * 7))
                        .build();
                    
                    List<OrderItem> orderItems = new ArrayList<>();
                    BigDecimal totalPrice = BigDecimal.ZERO;
                    
                    // Add 2-4 items per order
                    int itemCount = 2 + (orderCount % 3);
                    for (int i = 0; i < itemCount; i++) {
                        Menu menu = menuItems.get((orderCount * itemCount + i) % menuItems.size());
                        int quantity = 1 + (i % 2);
                        BigDecimal itemPrice = menu.getPrice().multiply(BigDecimal.valueOf(quantity));
                        
                        OrderItem orderItem = OrderItem.builder()
                            .order(order)
                            .menu(menu)
                            .quantity(quantity)
                            .pricePerUnit(menu.getPrice())
                            .subtotal(itemPrice)
                            .build();
                        
                        orderItems.add(orderItem);
                        totalPrice = totalPrice.add(itemPrice);
                    }
                    
                    order.setOrderItems(orderItems);
                    order.setTotalAmount(totalPrice);
                    orders.add(order);
                }
            }
        }

        orderRepository.saveAll(orders);
        log.info("Initialized {} orders with order items", orders.size());
    }

    private void initializePayments() {
        List<Order> orders = orderRepository.findAll();
        List<Payment> payments = new ArrayList<>();

        for (Order order : orders) {
            // Only create payments for CONFIRMED orders
            if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
                Payment payment = Payment.builder()
                    .user(order.getUser())
                    .order(order)
                    .amount(order.getTotalAmount())
                    .paymentGateway(PaymentGateway.STRIPE)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .transactionId("ch_" + System.currentTimeMillis() + "_" + order.getId())
                    .paymentDate(order.getOrderDate().plusMinutes(5))
                    .build();
                
                payments.add(payment);
            }
        }

        paymentRepository.saveAll(payments);
        log.info("Initialized {} payments", payments.size());
    }

    private void initializeReviews() {
        // Reviews seeding removed as part of menu/categories/cart deprecation and refactor to resource-based reviews.
        // Intentionally left blank.
    }
}