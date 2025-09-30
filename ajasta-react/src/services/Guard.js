import { Navigate, useLocation } from "react-router-dom"
import ApiService from "./ApiService"




export const CustomerRoute = ({element: Component}) => {
   
    const location = useLocation()
    
    return ApiService.isAuthenticated() && ApiService.isCustomer() ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}

export const AdminRoute = ({element: Component}) => {
    
    const location = useLocation()
    return ApiService.isAuthenticated() && ApiService.isAdmin() ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}

export const DeliveryRoute = ({element: Component}) => {
    
    const location = useLocation()
    return ApiService.isAuthenticated() && ApiService.isDeliveryPerson() ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}

export const ProtectedRoute = ({element: Component}) => {
    const location = useLocation()
    return ApiService.isAuthenticated() ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}
