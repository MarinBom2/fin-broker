import { BrowserRouter, Route, Routes } from 'react-router-dom'
import CompanyDetailPage from './pages/company/CompanyDetailPage'
import CompanyListPage from './pages/company/CompanyListPage'
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/login/LoginPage'
import SafetyPage from './pages/safety/SafetyPage'
import { ROUTES } from './routes'
import { AuthProvider } from './shared/auth/AuthContext'
import { RequireAuth } from './shared/auth/RequireAuth'
import { AppHeader } from './shared/ui/layout/AppHeader'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppHeader />
        <main className="page">
          <Routes>
            <Route path={ROUTES.SCR01_HOME} element={<HomePage />} />
            <Route
              path={ROUTES.SCR05_COMPANIES}
              element={
                <RequireAuth>
                  <CompanyListPage />
                </RequireAuth>
              }
            />
            <Route
              path={ROUTES.SCR05_COMPANY}
              element={
                <RequireAuth>
                  <CompanyDetailPage />
                </RequireAuth>
              }
            />
            <Route
              path={ROUTES.SCR06_SAFETY}
              element={
                <RequireAuth>
                  <SafetyPage />
                </RequireAuth>
              }
            />
            <Route path={ROUTES.SCR07_LOGIN} element={<LoginPage />} />
            <Route path="*" element={<HomePage />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  )
}
