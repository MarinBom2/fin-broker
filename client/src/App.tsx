import type { ReactNode } from 'react'
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom'
import CompanyDetailPage from './pages/company/CompanyDetailPage'
import CompanyListPage from './pages/company/CompanyListPage'
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/login/LoginPage'
import SafetyPage from './pages/safety/SafetyPage'
import { ROUTES } from './routes'
import { AuthProvider } from './shared/auth/AuthContext'
import { RequireAuth } from './shared/auth/RequireAuth'
import { AppFooter } from './shared/ui/layout/AppFooter'
import { AppHeader } from './shared/ui/layout/AppHeader'
import { TabBar } from './shared/ui/layout/TabBar'

/** 홈은 가로 전체를 쓰고, 나머지 화면은 본문 폭(980px) 안에 둔다 */
function Shell({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const wide = pathname === ROUTES.SCR01_HOME
  return (
    <div className="app">
      <AppHeader />
      {/* key 로 화면이 바뀔 때마다 등장 애니메이션을 다시 돌린다 */}
      <main key={pathname} className={wide ? 'main main--wide' : 'main'}>
        {children}
      </main>
      <AppFooter />
      <TabBar />
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Shell>
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
        </Shell>
      </AuthProvider>
    </BrowserRouter>
  )
}
