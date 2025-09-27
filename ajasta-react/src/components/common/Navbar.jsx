import { useNavigate, Link } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import ApiService from "../../services/ApiService";

const Navbar = () => {
    const { t, i18n } = useTranslation();
    const [showLanguageDropdown, setShowLanguageDropdown] = useState(false);

    const isAuthenticated = ApiService.isAuthenticated();
    const isAdmin = ApiService.isAdmin();
    const isCustomer = ApiService.isCustomer();
    const isDeliveryPerson = ApiService.isDeliveryPerson();
    const navigate = useNavigate();

    const languages = [
        { code: 'en', name: t('english'), flag: '🇺🇸' },
        { code: 'et', name: t('estonian'), flag: '🇪🇪' },
        { code: 'es', name: t('spanish'), flag: '🇪🇸' },
        { code: 'fr', name: t('french'), flag: '🇫🇷' },
        { code: 'pt', name: t('portuguese'), flag: '🇵🇹' },
        { code: 'it', name: t('italian'), flag: '🇮🇹' },
        { code: 'ru', name: t('russian'), flag: '🇷🇺' }
    ];

    const currentLanguage = languages.find(lang => lang.code === i18n.language) || languages[0];

    const handleLanguageChange = (languageCode) => {
        i18n.changeLanguage(languageCode);
        setShowLanguageDropdown(false);
    };

    const handleLogout = () => {
        const isLogout = window.confirm(t('logout_confirm'));
        if (isLogout) {
            ApiService.logout();
            navigate("/login")
        }
    }


    return (
        <nav>
            <div className="logo">
                <Link to="/" className="logo-link">
                    {t('app_title')}</Link>
            </div>

            <div className="desktop-nav">
                <Link to="/home" className="nav-link">{t('home')}</Link>
                <Link to="/menu" className="nav-link">{t('menu')}</Link>
                <Link to="/resources" className="nav-link">{t('resources')}</Link>
                <Link to="/categories" className="nav-link">{t('categories')}</Link>

                {/* Language Dropdown */}
                <div className="language-dropdown" style={{ position: 'relative', display: 'inline-block' }}>
                    <button 
                        className="nav-button language-button"
                        onClick={() => setShowLanguageDropdown(!showLanguageDropdown)}
                        style={{ 
                            background: 'none', 
                            border: 'none', 
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '5px',
                            color: 'inherit',
                            fontSize: 'inherit'
                        }}
                    >
                        <span>{currentLanguage.flag}</span>
                        <span>{t('language')}</span>
                        <span style={{ fontSize: '12px' }}>▼</span>
                    </button>
                    
                    {showLanguageDropdown && (
                        <div 
                            className="language-dropdown-menu"
                            style={{
                                position: 'absolute',
                                top: '100%',
                                right: '0',
                                backgroundColor: 'white',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
                                zIndex: 1000,
                                minWidth: '150px'
                            }}
                        >
                            {languages.map((language) => (
                                <button
                                    key={language.code}
                                    onClick={() => handleLanguageChange(language.code)}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px',
                                        width: '100%',
                                        padding: '8px 12px',
                                        border: 'none',
                                        background: i18n.language === language.code ? '#f0f0f0' : 'white',
                                        cursor: 'pointer',
                                        fontSize: '14px',
                                        textAlign: 'left'
                                    }}
                                    onMouseEnter={(e) => e.target.style.backgroundColor = '#f5f5f5'}
                                    onMouseLeave={(e) => e.target.style.backgroundColor = i18n.language === language.code ? '#f0f0f0' : 'white'}
                                >
                                    <span>{language.flag}</span>
                                    <span>{language.name}</span>
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {isAuthenticated ? (
                    <>
                        {isCustomer && (
                            <>
                                <Link to="/my-order-history" className="nav-link">{t('orders')}</Link>
                                <Link to="/cart" className="nav-link">{t('cart')}</Link>
                            </>
                        )}
                        {isDeliveryPerson && (
                            <Link to="/deliveries" className="nav-link">{t('deliveries')}</Link>
                        )}
                        {isAdmin && (
                            <Link to="/admin" className="nav-link">{t('admin')}</Link>
                        )}
                        <Link to="/profile" className="nav-link">{t('profile')}</Link>
                        <button className="nav-button" onClick={handleLogout}>
                            {t('logout')}
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login" className="nav-link">{t('login')}</Link>
                        <Link to="/register" className="nav-link">{t('register')}</Link>
                    </>
                )}
            </div>
        </nav>
    )



}
export default Navbar;










