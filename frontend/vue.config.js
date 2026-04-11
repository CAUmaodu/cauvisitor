module.exports = {
  devServer: {
    proxy: {
      '/cauvisitor': {
        target: process.env.VUE_APP_BACKEND_PROXY_TARGET || 'http://localhost:8081',
        changeOrigin: true,
        pathRewrite: {
          '^/cauvisitor': '/cauvisitor'
        }
      }
    }
  }
}