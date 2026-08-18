/**
 * EntityKart AngularJS Application Config
 * v3.1.0 — Monolith Local (port 8080)
 *
 * APK: MainActivity discovers gateway IP on local WiFi and injects it.
 * Browser: Always uses localhost:8080 (monolith backend).
 */
var app = angular.module('entitykartApp', ['ngRoute']);

// Monolith backend port — hardcoded, do NOT read from localStorage
var LOCAL_GATEWAY_PORT = '8080';

// Clear any stale microservices port from old sessions
try { localStorage.removeItem('API_PORT'); } catch(e) {}

app.constant('API_BASE', (function () {
    if (typeof window !== 'undefined') {
        var protocol = window.location.protocol;
        var host     = window.location.hostname;

        // ── Mobile WebView / APK Context ──────────────────────────────────────
        if (protocol === 'file:' ||
            (window.AndroidConfig && window.AndroidConfig.apiBase) ||
            (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function')) {

            // 1. AndroidBridge — set by MainActivity after LAN discovery
            if (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function') {
                var bridgeUrl = window.AndroidBridge.getApiBase();
                if (bridgeUrl && bridgeUrl.trim().length > 0) { return bridgeUrl; }
            }
            // 2. AndroidConfig (legacy)
            if (window.AndroidConfig && window.AndroidConfig.apiBase) {
                return window.AndroidConfig.apiBase;
            }
            // 3. Window global injected by WebViewClient.onPageFinished
            if (window.ENTITYKART_API_BASE && window.ENTITYKART_API_BASE.trim().length > 0) {
                return window.ENTITYKART_API_BASE;
            }
            // 4. localStorage IP only (port always 8080)
            try {
                var savedIp = localStorage.getItem('API_IP');
                if (savedIp && savedIp.trim().length > 0) {
                    return 'http://' + savedIp + ':' + LOCAL_GATEWAY_PORT;
                }
            } catch (e) { /* ignore */ }

            return 'http://0.0.0.0:' + LOCAL_GATEWAY_PORT;
        }

        // ── Web Browser Mode — ALWAYS port 8080 ──────────────────────────────
        if (host === 'localhost' || host === '127.0.0.1') {
            return protocol + '//' + host + ':' + LOCAL_GATEWAY_PORT;
        }

        // ── Production (deployed behind Nginx) ────────────────────────────────
        return protocol + '//' + window.location.host;
    }
    return 'http://localhost:' + LOCAL_GATEWAY_PORT;
})());

// Route Configurations
app.config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {

    $routeProvider
        .when('/', {
            templateUrl: 'views/home.html',
            controller: 'productController'
        })
        .when('/products', {
            templateUrl: 'views/products.html',
            controller: 'productController'
        })
        .when('/product/:productId', {
            templateUrl: 'views/product-detail.html',
            controller: 'productController'
        })
        .when('/login', {
            templateUrl: 'views/login.html',
            controller: 'authController'
        })
        .when('/register', {
            templateUrl: 'views/register.html',
            controller: 'authController'
        })
        .when('/forgot-password', {
            templateUrl: 'views/forgot-password.html',
            controller: 'authController'
        })
        .when('/reset-password', {
            templateUrl: 'views/reset-password.html',
            controller: 'authController'
        })
        .when('/cart', {
            templateUrl: 'views/cart.html',
            controller: 'cartController'
        })
        .when('/checkout', {
            templateUrl: 'views/checkout.html',
            controller: 'cartController'
        })
        .when('/orders', {
            templateUrl: 'views/orders.html',
            controller: 'orderController'
        })
        .when('/returns', {
            templateUrl: 'views/returns.html',
            controller: 'returnController'
        })
        .when('/wishlist', {
            templateUrl: 'views/wishlist.html',
            controller: 'wishlistController'
        })
        .when('/admin', {
            templateUrl: 'views/admin.html',
            controller: 'adminController'
        })
        .when('/profile', {
            templateUrl: 'views/profile.html',
            controller: 'profileController'
        })
        .otherwise({
            redirectTo: '/'
        });

    // Add interceptor to pass authentication tokens and handle common response statuses globally
    $httpProvider.interceptors.push('apiInterceptor');

    // Configure default CSRF settings
    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
}]);

// Global app initialization
app.run(['$rootScope', '$location', 'authService', function ($rootScope, $location, authService) {
    // Restore auth state from localStorage BEFORE any route guard fires.
    authService.init();

    // Pages that are always publicly accessible — never redirect these
    var publicPages = ['/login', '/register', '/forgot-password', '/reset-password', '/products', '/product', '/'];

    // Pages that require a logged-in user
    var restrictedPages = ['/checkout', '/orders', '/returns', '/wishlist', '/admin', '/profile'];

    $rootScope.$on('$routeChangeStart', function (event, next, current) {
        var path = $location.path();

        // --- Guard: Redirect already logged-in users visiting auth pages ---
        var authPages = ['/login', '/register', '/forgot-password', '/reset-password'];
        var isAuthPage = authPages.some(function (page) {
            return path === page;
        });
        if (isAuthPage && authService.isLoggedIn()) {
            event.preventDefault();
            $location.path('/');
            return;
        }

        // --- Guard: Never block access to public pages ---
        var isPublic = publicPages.some(function (page) {
            return path === page || path.indexOf(page) === 0;
        });
        if (isPublic) {
            return;
        }

        // --- Guard: Restricted pages need authentication ---
        var isRestricted = restrictedPages.some(function (page) {
            return path.indexOf(page) === 0;
        });

        if (isRestricted && !authService.isLoggedIn()) {
            event.preventDefault();
            $rootScope.$broadcast('showToast', {
                title: 'Login Required',
                message: 'Please sign in to access this page.',
                type: 'error'
            });
            $location.path('/login');
            return;
        }

        // --- Guard: Admin section needs ADMIN role ---
        if (path.indexOf('/admin') === 0 && !authService.isAdmin()) {
            event.preventDefault();
            $rootScope.$broadcast('showToast', {
                title: 'Access Denied',
                message: 'Admin privileges required.',
                type: 'error'
            });
            $location.path('/');
        }
    });
}]);
