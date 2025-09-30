import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/common/Navbar";
import Footer from "./components/common/Footer";
import './i18n'; // Initialize i18n
import RegisterPage from "./components/auth/RegisterPage";
import LoginPage from "./components/auth/LoginPage";
import ProfilePage from "./components/profile_cart/ProfilePage";
import UpdateProfilePage from "./components/profile_cart/UpdateProfilePage";
import OrderHistoryPage from "./components/profile_cart/OrderHistoryPage";
import { AdminRoute, CustomerRoute, ProtectedRoute } from "./services/Guard";
import ProcessPaymenttPage from "./components/payment/ProcessPaymenttPage";
import AdminLayout from "./components/admin/navbar/AdminLayout";
import AdminOrdersPage from "./components/admin/AdminOrdersPage";
import AdminOrderDetailPage from "./components/admin/AdminOrderDetailPage";
import AdminPaymentsPage from "./components/admin/AdminPaymentsPage";
import AdminPaymentDetailPage from "./components/admin/AdminPaymentDetailPage";
import AdminDashboardPage from "./components/admin/AdminDashboardPage";
import AdminUserRegistration from "./components/auth/AdminUserRegistration";
import ResourcesPage from "./components/resources/ResourcesPage";
import ResourceDetailsPage from "./components/resources/ResourceDetailsPage";
import AdminResourcesPage from "./components/admin/AdminResourcesPage";
import AdminResourceFormPage from "./components/admin/AdminResourceFormPage";
import ResourceBookingPage from "./components/resources/ResourceBookingPage";
import HomePage from "./components/home/HomePage";

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <div className="content">

        <Routes>

          {/* AUTH PAGES */}
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<LoginPage />} />

          <Route path="/" element={<HomePage />} />
          <Route path="/resources" element={<ResourcesPage />} />
          <Route path="/resources/:id" element={<ResourceDetailsPage />} />
          <Route path="/resources/:id/book" element={<ResourceBookingPage />} />

          <Route path="/profile" element={<ProtectedRoute element={<ProfilePage />} />} />

          <Route path="/update" element={<ProtectedRoute element={<UpdateProfilePage />} />} />

          <Route path="/my-order-history" element={<CustomerRoute element={<OrderHistoryPage />} />} />


          <Route path="/pay" element={<CustomerRoute element={<ProcessPaymenttPage />} />} />


          {/* ADMIN ROUTES */}

          <Route path="/admin" element={<AdminRoute element={<AdminLayout />} />}>

            <Route path="resources" element={<AdminResourcesPage />} />
            <Route path="resources/new" element={<AdminResourceFormPage />} />
            <Route path="resources/edit/:id" element={<AdminResourceFormPage />} />

            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="orders/:id" element={<AdminOrderDetailPage />} />

            <Route path="payments" element={<AdminPaymentsPage />} />
            <Route path="payments/:id" element={<AdminPaymentDetailPage />} />

            <Route index element={<AdminDashboardPage />} />
            <Route path="register" element={<AdminUserRegistration />} />
          </Route>

          <Route path="*" element={<Navigate to={"/"} />} />

        </Routes>

      </div>
      <Footer />
    </BrowserRouter>
  );
}

export default App;
