import { useNavigate, Link } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { useError } from "../common/ErrorDisplay"
import { useState } from "react";
import ApiService from "../../services/ApiService";

const RegisterPage = () => {
    const { t } = useTranslation();
    const { ErrorDisplay, showError } = useError();
    const navigate = useNavigate();

    const [formData, setFormData] = useState({

        name: '',
        email: '',
        password: '',
        phoneNumber: '',
        address: '',
        confirmPassword: '',

    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    }

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (
            !formData.name ||
            !formData.email ||
            !formData.password ||
            !formData.phoneNumber ||
            !formData.confirmPassword ||
            !formData.address
        ) {
            showError("All fields are required")
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            showError('Passwords do not match.');
            return;
        }

        const registrationData = {

            name: formData.name,
            email: formData.email,
            password: formData.password,
            phoneNumber: formData.phoneNumber,
            address: formData.address,
            roles: ['CUSTOMER']

        };

        try {
            const response = await ApiService.registerUser(registrationData);
            if (response.statusCode === 200) {
                setFormData({
                    name: '', email: '', password: '', phoneNumber: '', address: '', confirmPassword: ''
                });
                navigate("/login")
            } else {
                showError(response.message)
            }
        } catch (error) {
            showError(error.response?.data?.message || error.message);
        }
    };


    return (
        <div className="register-page-ajasta">
            <div className="register-card-ajasta">
                <div className="register-header-ajasta">
                    <h2 className="register-title-ajasta">{t('register_title')}</h2>
                    <p className="register-description-ajasta">{t('register_description')}</p>
                </div>
                <div className="register-content-ajasta">
                    
                    <form className="register-form-ajasta" onSubmit={handleSubmit}>
                       
                        <div className="register-form-group">
                            <label htmlFor="name" className="register-label-ajasta">{t('full_name')}</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                required
                                placeholder={t('your_full_name')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="email" className="register-label-ajasta">{t('email')}</label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                placeholder={t('your_email_here')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="password" className="register-label-ajasta">{t('password')}</label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                                placeholder={t('password')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="confirmPassword" className="register-label-ajasta">{t('confirm_password')}</label>
                            <input
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                required
                                placeholder={t('confirm_password')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="phoneNumber" className="register-label-ajasta">{t('phone_number')}</label>
                            <input
                                type="text"
                                id="phoneNumber"
                                name="phoneNumber"
                                value={formData.phoneNumber}
                                onChange={handleChange}
                                required
                                placeholder={t('your_phone_number')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="address" className="register-label-ajasta">{t('address')}</label>
                            <input
                                type="text"
                                id="address"
                                name="address"
                                value={formData.address}
                                onChange={handleChange}
                                required
                                placeholder={t('your_address_here')}
                                className="register-input-ajasta"

                            />
                        </div>

                        <ErrorDisplay/>

                        <div>
                            <button type="submit" className="register-button-ajasta">
                                {t('register')}
                            </button>
                        </div>

                        <div className="already">
                            <Link to="/login" className="register-link-ajasta">
                                {t('already_have_account')}
                            </Link>
                        </div>

                    </form>

                    <div className="register-social-ajasta">
                        <div className="register-separator-ajasta">
                            <span className="register-separator-text-ajasta">{t('or_continue_with')}</span>
                        </div>

                        <div className="register-social-buttons-ajasta">
                            {/* Add social login buttons here (e.g., Google, Facebook, GitHub) */}
                            <button className="register-social-button-ajasta register-social-google-ajasta">Google</button>
                            <button className="register-social-button-ajasta register-social-facebook-ajasta">Facebook</button>
                            <button className="register-social-button-ajasta register-social-github-ajasta">Github</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default RegisterPage;