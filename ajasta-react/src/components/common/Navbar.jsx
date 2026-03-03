import { useNavigate, Link } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useAuth } from "../../context/AuthContext";
import ApiService from "../../services/ApiService";

const Navbar = () => {
    const { t, i18n } = useTranslation();
    const [showLanguageDropdown, setShowLanguageDropdown] = useState(false);

    const navigate = useNavigate();

    // Use Keycloak authentication from AuthContext
    const { isAuthenticated, isLoading, isAdmin: keycloakIsAdmin, hasRole, login, logout, user } = useAuth();

    // Determine roles (prefer Keycloak roles)
    const isAdmin = keycloakIsAdmin || ApiService.isAdmin();
    const isCustomer = hasRole('user') || ApiService.isCustomer();
    const isResourceManager = hasRole('resource_manager') || ApiService.isResourceManager();

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
            // Logout from Keycloak
            logout();
            // Also clear any local storage
            ApiService.logout();
            navigate("/");
        }
    }

    const handleLogin = () => {
        // Redirect to Keycloak login
        login();
    }

    // Show loading state while checking authentication
    if (isLoading) {
        return (
            <nav>
                <div className="logo">
                    <Link to="/" className="logo-link">{t('app_title')}</Link>
                </div>
                <div className="desktop-nav">
                    <Link to="/" className="nav-link">{t('home')}</Link>
                    <Link to="/resources" className="nav-link">{t('resources')}</Link>
                </div>
            </nav>
        );
    }

    return (
        <nav>
            <div className="logo">
                <Link to="/" className="logo-link">
                    {t('app_title')}</Link>
            </div>

            <div className="desktop-nav">
                <Link to="/" className="nav-link">{t('home')}</Link>
                <Link to="/resources" className="nav-link">{t('resources')}</Link>
                <Link to="/cms" className="nav-link">CMS</Link>

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
                            </>
                        )}
                        {(isAdmin || isResourceManager) && (
                            <Link to="/admin" className="nav-link">{t('admin')}</Link>
                        )}
                        <Link to="/profile" className="nav-link">
                            {user?.fullName || user?.username || t('profile')}
                        </Link>
                        <button className="nav-button" onClick={handleLogout}>
                            {t('logout')}
                        </button>
                    </>
                ) : (
                    <>
                        <button className="nav-button" onClick={handleLogin}>
                            {t('login')}
                        </button>
                    </>
                )}
            </div>
        </nav>
    )
}

export default Navbar;
