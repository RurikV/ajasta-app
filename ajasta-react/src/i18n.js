import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Translation resources
const resources = {
  en: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Home",
      "menu": "Menu",
      "categories": "Categories",
      "resources": "Resources",
      "orders": "Orders",
      "cart": "Cart",
      "deliveries": "Deliveries",
      "admin": "Admin",
      "profile": "Profile",
      "logout": "Logout",
      "login": "Login",
      "register": "Register",
      "logout_confirm": "Are you sure you want to logout?",
      
      // Language names
      "language": "Language",
      "english": "English",
      "estonian": "Estonian",
      "spanish": "Spanish",
      "french": "French",
      "portuguese": "Portuguese",
      "italian": "Italian",
      "russian": "Russian",
      
      // Authentication
      "login_title": "Login",
      "login_description": "Login to your account to order delicious ajasta!",
      "register_title": "Register",
      "register_description": "Create an account to order delicious ajasta!",
      "email": "Email",
      "password": "Password",
      "full_name": "Full Name",
      "confirm_password": "Confirm Password",
      "phone_number": "Phone Number",
      "address": "Address",
      "your_email_address": "Your Email Address",
      "your_full_name": "Your Full Name",
      "your_email_here": "Your Email Here",
      "your_phone_number": "Your Phone Number",
      "your_address_here": "Your Address Here",
      "dont_have_account": "Don't Have an Account? Register",
      "already_have_account": "Already Have an Account? Login",
      "or_continue_with": "Or continue with"
    }
  },
  et: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Avaleht",
      "menu": "Menüü",
      "categories": "Kategooriad",
      "orders": "Tellimused",
      "cart": "Ostukorv",
      "deliveries": "Tarned",
      "admin": "Administraator",
      "profile": "Profiil",
      "logout": "Välju",
      "login": "Sisene",
      "register": "Registreeru",
      "logout_confirm": "Kas olete kindel, et soovite välja logida?",
      
      // Language names
      "language": "Keel",
      "english": "Inglise",
      "estonian": "Eesti",
      "spanish": "Hispaania",
      "french": "Prantsuse",
      "portuguese": "Portugali",
      "italian": "Itaalia",
      "russian": "Vene",
      
      // Authentication
      "login_title": "Sisselogimine",
      "login_description": "Logige oma kontole sisse, et tellida maitsvat toitu!",
      "register_title": "Registreerimine",
      "register_description": "Looge konto, et tellida maitsvat toitu!",
      "email": "E-post",
      "password": "Parool",
      "full_name": "Täisnimi",
      "confirm_password": "Kinnita parool",
      "phone_number": "Telefoninumber",
      "address": "Aadress",
      "your_email_address": "Teie e-posti aadress",
      "your_full_name": "Teie täisnimi",
      "your_email_here": "Teie e-post siia",
      "your_phone_number": "Teie telefoninumber",
      "your_address_here": "Teie aadress siia",
      "dont_have_account": "Pole kontot? Registreeruge",
      "already_have_account": "On juba konto? Logige sisse",
      "or_continue_with": "Või jätkake"
    }
  },
  es: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Inicio",
      "menu": "Menú",
      "categories": "Categorías",
      "orders": "Pedidos",
      "cart": "Carrito",
      "deliveries": "Entregas",
      "admin": "Administrador",
      "profile": "Perfil",
      "logout": "Cerrar sesión",
      "login": "Iniciar sesión",
      "register": "Registrarse",
      "logout_confirm": "¿Estás seguro de que quieres cerrar sesión?",
      
      // Language names
      "language": "Idioma",
      "english": "Inglés",
      "estonian": "Estonio",
      "spanish": "Español",
      "french": "Francés",
      "portuguese": "Portugués",
      "italian": "Italiano",
      "russian": "Ruso",
      
      // Authentication
      "login_title": "Iniciar sesión",
      "login_description": "¡Inicia sesión en tu cuenta para pedir comida deliciosa!",
      "register_title": "Registrarse",
      "register_description": "¡Crea una cuenta para pedir comida deliciosa!",
      "email": "Correo electrónico",
      "password": "Contraseña",
      "full_name": "Nombre completo",
      "confirm_password": "Confirmar contraseña",
      "phone_number": "Número de teléfono",
      "address": "Dirección",
      "your_email_address": "Tu dirección de correo",
      "your_full_name": "Tu nombre completo",
      "your_email_here": "Tu correo aquí",
      "your_phone_number": "Tu número de teléfono",
      "your_address_here": "Tu dirección aquí",
      "dont_have_account": "¿No tienes cuenta? Regístrate",
      "already_have_account": "¿Ya tienes cuenta? Inicia sesión",
      "or_continue_with": "O continúa con"
    }
  },
  fr: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Accueil",
      "menu": "Menu",
      "categories": "Catégories",
      "orders": "Commandes",
      "cart": "Panier",
      "deliveries": "Livraisons",
      "admin": "Administrateur",
      "profile": "Profil",
      "logout": "Se déconnecter",
      "login": "Se connecter",
      "register": "S'inscrire",
      "logout_confirm": "Êtes-vous sûr de vouloir vous déconnecter ?",
      
      // Language names
      "language": "Langue",
      "english": "Anglais",
      "estonian": "Estonien",
      "spanish": "Espagnol",
      "french": "Français",
      "portuguese": "Portugais",
      "italian": "Italien",
      "russian": "Russe",
      
      // Authentication
      "login_title": "Connexion",
      "login_description": "Connectez-vous à votre compte pour commander de la nourriture délicieuse !",
      "register_title": "Inscription",
      "register_description": "Créez un compte pour commander de la nourriture délicieuse !",
      "email": "E-mail",
      "password": "Mot de passe",
      "full_name": "Nom complet",
      "confirm_password": "Confirmer le mot de passe",
      "phone_number": "Numéro de téléphone",
      "address": "Adresse",
      "your_email_address": "Votre adresse e-mail",
      "your_full_name": "Votre nom complet",
      "your_email_here": "Votre e-mail ici",
      "your_phone_number": "Votre numéro de téléphone",
      "your_address_here": "Votre adresse ici",
      "dont_have_account": "Pas de compte ? Inscrivez-vous",
      "already_have_account": "Déjà un compte ? Connectez-vous",
      "or_continue_with": "Ou continuez avec"
    }
  },
  pt: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Início",
      "menu": "Menu",
      "categories": "Categorias",
      "orders": "Pedidos",
      "cart": "Carrinho",
      "deliveries": "Entregas",
      "admin": "Administrador",
      "profile": "Perfil",
      "logout": "Sair",
      "login": "Entrar",
      "register": "Registrar",
      "logout_confirm": "Tem certeza de que deseja sair?",
      
      // Language names
      "language": "Idioma",
      "english": "Inglês",
      "estonian": "Estoniano",
      "spanish": "Espanhol",
      "french": "Francês",
      "portuguese": "Português",
      "italian": "Italiano",
      "russian": "Russo",
      
      // Authentication
      "login_title": "Login",
      "login_description": "Faça login na sua conta para pedir comida deliciosa!",
      "register_title": "Registrar",
      "register_description": "Crie uma conta para pedir comida deliciosa!",
      "email": "E-mail",
      "password": "Senha",
      "full_name": "Nome completo",
      "confirm_password": "Confirmar senha",
      "phone_number": "Número de telefone",
      "address": "Endereço",
      "your_email_address": "Seu endereço de e-mail",
      "your_full_name": "Seu nome completo",
      "your_email_here": "Seu e-mail aqui",
      "your_phone_number": "Seu número de telefone",
      "your_address_here": "Seu endereço aqui",
      "dont_have_account": "Não tem conta? Registre-se",
      "already_have_account": "Já tem conta? Faça login",
      "or_continue_with": "Ou continue com"
    }
  },
  it: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Home",
      "menu": "Menu",
      "categories": "Categorie",
      "orders": "Ordini",
      "cart": "Carrello",
      "deliveries": "Consegne",
      "admin": "Amministratore",
      "profile": "Profilo",
      "logout": "Esci",
      "login": "Accedi",
      "register": "Registrati",
      "logout_confirm": "Sei sicuro di voler uscire?",
      
      // Language names
      "language": "Lingua",
      "english": "Inglese",
      "estonian": "Estone",
      "spanish": "Spagnolo",
      "french": "Francese",
      "portuguese": "Portoghese",
      "italian": "Italiano",
      "russian": "Russo",
      
      // Authentication
      "login_title": "Accesso",
      "login_description": "Accedi al tuo account per ordinare cibo delizioso!",
      "register_title": "Registrazione",
      "register_description": "Crea un account per ordinare cibo delizioso!",
      "email": "E-mail",
      "password": "Password",
      "full_name": "Nome completo",
      "confirm_password": "Conferma password",
      "phone_number": "Numero di telefono",
      "address": "Indirizzo",
      "your_email_address": "Il tuo indirizzo e-mail",
      "your_full_name": "Il tuo nome completo",
      "your_email_here": "La tua e-mail qui",
      "your_phone_number": "Il tuo numero di telefono",
      "your_address_here": "Il tuo indirizzo qui",
      "dont_have_account": "Non hai un account? Registrati",
      "already_have_account": "Hai già un account? Accedi",
      "or_continue_with": "Oppure continua con"
    }
  },
  ru: {
    translation: {
      // Navbar
      "app_title": "Ajasta App",
      "home": "Главная",
      "menu": "Меню",
      "categories": "Категории",
      "orders": "Заказы",
      "cart": "Корзина",
      "deliveries": "Доставки",
      "admin": "Администратор",
      "profile": "Профиль",
      "logout": "Выйти",
      "login": "Войти",
      "register": "Регистрация",
      "logout_confirm": "Вы уверены, что хотите выйти?",
      
      // Language names
      "language": "Язык",
      "english": "Английский",
      "estonian": "Эстонский",
      "spanish": "Испанский",
      "french": "Французский",
      "portuguese": "Португальский",
      "italian": "Итальянский",
      "russian": "Русский",
      
      // Authentication
      "login_title": "Вход",
      "login_description": "Войдите в свой аккаунт, чтобы заказать вкусную еду!",
      "register_title": "Регистрация",
      "register_description": "Создайте аккаунт, чтобы заказать вкусную еду!",
      "email": "Электронная почта",
      "password": "Пароль",
      "full_name": "Полное имя",
      "confirm_password": "Подтвердите пароль",
      "phone_number": "Номер телефона",
      "address": "Адрес",
      "your_email_address": "Ваш адрес электронной почты",
      "your_full_name": "Ваше полное имя",
      "your_email_here": "Ваша почта здесь",
      "your_phone_number": "Ваш номер телефона",
      "your_address_here": "Ваш адрес здесь",
      "dont_have_account": "Нет аккаунта? Зарегистрируйтесь",
      "already_have_account": "Уже есть аккаунт? Войдите",
      "or_continue_with": "Или продолжите с"
    }
  }
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: localStorage.getItem('language') || 'en', // Default to English
    fallbackLng: 'en',
    
    interpolation: {
      escapeValue: false, // React already escapes values
    },
    
    // Save language preference to localStorage
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    }
  });

// Save language to localStorage when changed
i18n.on('languageChanged', (lng) => {
  localStorage.setItem('language', lng);
});

export default i18n;