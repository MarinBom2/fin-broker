import { BrowserRouter, Route, Routes } from 'react-router-dom'
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/login/LoginPage'
import { ROUTES } from './routes'
import { AuthProvider } from './shared/auth/AuthContext'
import { AppHeader } from './shared/ui/layout/AppHeader'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppHeader />
        <main className="page">
          <Routes>
            <Route path={ROUTES.SCR01_HOME} element={<HomePage />} />
            <Route path={ROUTES.SCR07_LOGIN} element={<LoginPage />} />
            <Route path="*" element={<HomePage />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  )
}
