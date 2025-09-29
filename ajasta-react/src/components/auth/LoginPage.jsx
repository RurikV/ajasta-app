import { useNavigate, Link, useLocation } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { useError } from "../common/ErrorDisplay"
import { useState } from "react";
import ApiService from "../../services/ApiService";

const LoginPage = () => {
    const { t } = useTranslation();
    const { ErrorDisplay, showError } = useError();
    const navigate = useNavigate();
    const { state } = useLocation();
    const redirectPath = state?.from?.pathname || "/home"

    const [formData, setFormData] = useState({

        email: '',
        password: ''

    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    }

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.email || !formData.password) {
            showError('Email and password are required.');
            return;
        }

        try {
            const response = await ApiService.loginUser(formData);
            if (response.statusCode === 200) {
                ApiService.saveToken(response.data.token)
                // Cache roles in-memory (no localStorage) for immediate UI role checks
                ApiService.saveRole(response.data.roles)
                navigate(redirectPath, {replace: true})
            } else {
                showError(response.message)
            }
        } catch (error) {
            showError(error.response?.data?.message || error.message);
        }
    };


    return (
        <div className="login-page-ajasta">
        {/* Render the ErrorDisplay component */}
        <ErrorDisplay />
            <div className="login-card-ajasta">
                
                <div className="login-header-ajasta">
                    <h2 className="login-title-ajasta">{t('login_title')}</h2>
                    <p className="login-description-ajasta">{t('login_description')}</p>
                </div>

                <div className="login-content-ajasta">
                    <form className="login-form-ajasta" onSubmit={handleSubmit}>
                        <div className="login-form-group">
                            <label htmlFor="email" className="login-label-ajasta">{t('email')}</label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                autoComplete="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                placeholder={t('your_email_address')}
                                className="login-input-ajasta"
                            />
                        </div>
                        <div className="login-form-group">
                            <label htmlFor="password" className="login-label-ajasta">{t('password')}</label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                                placeholder={t('password')}
                                className="login-input-ajasta"
                            />
                        </div>

                        <div>
                            <button type="submit" className="login-button-ajasta">
                                {t('login')}
                            </button>
                        </div>


                        <div className="already">
                            <Link to="/register" className="register-link-ajasta">
                                {t('dont_have_account')}
                            </Link>
                        </div>
                    </form>

                    <div className="login-social-ajasta">
                        <div className="login-separator-ajasta">
                            <span className="login-separator-text-ajasta">{t('or_continue_with')}</span>
                        </div>

                        <div className="login-social-buttons-ajasta">
                            {/* Add social login buttons here (e.g., Google, Facebook, GitHub) */}
                            <button className="login-social-button-ajasta login-social-google-ajasta">Google</button>
                            <button className="login-social-button-ajasta login-social-facebook-ajasta">Facebook</button>
                            <button className="login-social-button-ajasta login-social-github-ajasta">Github</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );



}
export default LoginPage;