# 网页端部署指南

## 部署选项

### 选项一：LeanCloud 云引擎（推荐）

LeanCloud 云引擎提供完整的 Node.js 环境，适合部署 Vue 应用。

#### 部署步骤：

1. **安装 LeanCloud CLI**
   ```bash
   npm install -g lean-cli
   ```

2. **登录 LeanCloud**
   ```bash
   lean login
   ```

3. **初始化云引擎项目**
   ```bash
   cd web
   lean init
   ```

4. **创建 `leanengine.yaml` 配置文件**
   ```yaml
   # leanengine.yaml
   app-id: your-app-id
   app-key: your-app-key
   master-key: your-master-key
   hook-key: your-hook-key
   ```

5. **创建 `server.js` 服务器文件**
   ```javascript
   const express = require('express')
   const path = require('path')
   const app = express()
   
   // 静态文件服务
   app.use(express.static(path.join(__dirname, 'dist')))
   
   // 所有路由返回 index.html
   app.get('*', (req, res) => {
     res.sendFile(path.join(__dirname, 'dist', 'index.html'))
   })
   
   const PORT = process.env.LEANCLOUD_APP_PORT || 3000
   app.listen(PORT, () => {
     console.log(`Server running on port ${PORT}`)
   })
   ```

6. **修改 `package.json` 添加云引擎脚本**
   ```json
   {
     "scripts": {
       "start": "node server.js",
       "deploy": "npm run build && lean deploy"
     }
   }
   ```

7. **部署到云引擎**
   ```bash
   npm run deploy
   ```

### 选项二：腾讯云静态网站托管

腾讯云 COS + CDN 提供高性能的静态网站托管。

#### 部署步骤：

1. **创建存储桶**
   - 登录腾讯云控制台
   - 进入 COS 服务
   - 创建存储桶，选择「静态网站」类型
   - 地域选择「中国大陆」（如上海、北京）

2. **配置静态网站**
   - 在存储桶的「基础配置」中开启「静态网站」
   - 设置索引文档为 `index.html`
   - 设置错误文档为 `index.html`（支持 Vue Router）

3. **配置 HTTPS**
   - 申请 SSL 证书（腾讯云提供免费证书）
   - 在 CDN 控制台绑定域名和证书

4. **构建并上传**
   ```bash
   # 构建项目
   npm run build
   
   # 使用 coscmd 上传
   coscmd config -a <secret-id> -s <secret-key> -b <bucket-name> -r <region>
   coscmd upload -r dist/ /
   ```

5. **配置自定义域名（可选）**
   - 在域名服务商添加 CNAME 记录
   - 在腾讯云 CDN 绑定域名

### 选项三：Vercel / Netlify（国际服务）

适合演示和测试，但国内访问可能较慢。

#### Vercel 部署：
```bash
# 安装 Vercel CLI
npm i -g vercel

# 部署
cd web
vercel
```

## HTTPS 配置

### LeanCloud 默认 HTTPS
- LeanCloud 云引擎默认提供 HTTPS
- 访问地址：`https://your-app.leanapp.cn`
- 无需额外配置

### 自定义域名 HTTPS
1. **购买域名**
   - 推荐国内服务商：阿里云、腾讯云
   - 选择 `.com` 或 `.cn` 域名

2. **ICP 备案（中国大陆必须）**
   - 如果使用国内服务器，必须备案
   - 备案流程约 10-20 个工作日
   - 备案期间可使用 LeanCloud 默认域名

3. **申请 SSL 证书**
   - 腾讯云/阿里云提供免费 SSL 证书
   - 申请后下载证书文件（包含 `.crt` 和 `.key`）

4. **配置证书**
   - **LeanCloud**：在控制台「设置」-「域名绑定」上传证书
   - **腾讯云**：在 CDN 控制台「证书管理」配置
   - **Nginx**：配置 SSL 证书路径

## 环境变量配置

### 开发环境
创建 `.env.development`：
```env
VITE_LEANCLOUD_APP_ID=your-dev-app-id
VITE_LEANCLOUD_APP_KEY=your-dev-app-key
VITE_LEANCLOUD_SERVER_URL=https://your-dev-server.lc-cn-n1-shared.com
```

### 生产环境
创建 `.env.production`：
```env
VITE_LEANCLOUD_APP_ID=your-prod-app-id
VITE_LEANCLOUD_APP_KEY=your-prod-app-key
VITE_LEANCLOUD_SERVER_URL=https://your-prod-server.lc-cn-n1-shared.com
```

### 在代码中使用
```javascript
const APP_ID = import.meta.env.VITE_LEANCLOUD_APP_ID
```

## 性能优化

### 构建优化
```javascript
// vite.config.js
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus']
        }
      }
    },
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  }
})
```

### CDN 配置
1. **开启 Gzip 压缩**
2. **配置浏览器缓存**
   - HTML: 不缓存或短时间缓存
   - JS/CSS: 长期缓存（使用 hash 文件名）
   - 图片: 长期缓存
3. **开启 HTTP/2**
4. **配置安全头部**
   ```nginx
   add_header X-Frame-Options SAMEORIGIN;
   add_header X-Content-Type-Options nosniff;
   add_header X-XSS-Protection "1; mode=block";
   ```

## 监控与日志

### LeanCloud 云引擎
- 控制台查看访问日志
- 配置错误报警
- 性能监控

### 自定义监控
1. **Google Analytics**（国际）
2. **百度统计**（国内）
3. **Sentry** 错误监控

## 备份与恢复

### 数据备份
1. **LeanCloud 数据导出**
   - 控制台定期导出数据
   - 使用 LeanCloud CLI 备份

2. **代码备份**
   - GitHub/GitLab 代码仓库
   - 定期备份构建产物

### 恢复流程
1. 重新部署代码
2. 导入备份数据
3. 验证功能正常

## 常见问题

### Q: 国内访问速度慢？
A: 
1. 使用国内节点（华东、华北）
2. 开启 CDN 加速
3. 优化图片和资源大小

### Q: HTTPS 证书过期？
A:
1. 设置证书过期提醒
2. 使用自动续期证书（Let's Encrypt）
3. 定期检查证书状态

### Q: 备案流程？
A:
1. 准备资料：身份证、域名、服务器信息
2. 在云服务商提交备案
3. 等待审核（10-20工作日）
4. 备案期间使用临时域名

## 安全建议

1. **API 密钥保护**
   - 不要将密钥提交到代码仓库
   - 使用环境变量
   - 定期轮换密钥

2. **输入验证**
   - 前端和后端都要验证输入
   - 防止 XSS 攻击
   - 使用 CSP 安全策略

3. **访问控制**
   - 用户认证和授权
   - 限制 API 访问频率
   - 记录操作日志

## 联系方式

- 项目文档：`README.md`
- 问题反馈：GitHub Issues
- 技术支持：your-email@example.com