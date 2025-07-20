import Header from '@/layouts/components/header';
// import Sidebar from '@/layouts/components';
import Footer from '@/layouts/components/footer';

export default function AppLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <div className="flex">
                {/*<Sidebar />*/}
                <main className="flex-1 p-6">
                    {children}
                </main>
            </div>
            <Footer />
        </div>
    );
}