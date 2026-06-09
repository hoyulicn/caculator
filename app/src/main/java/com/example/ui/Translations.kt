package com.example.ui

object Translations {
    private val zh = mapOf(
        "app_title" to "万能智能工具箱",
        "calc_sci" to "科学计算",
        "currency" to "汇率转换",
        "unit" to "单位换算",
        "date" to "日期计算",
        "finance" to "金融计算",
        "settings" to "系统设置",
        
        "shortcut_tint" to "长按或点击右上角齿轮自定义常用工具",
        "cust_shortcut" to "精选功能快捷栏",
        "add_shortcut" to "添加至快捷栏",
        "remove_shortcut" to "从快捷栏移除",
        "drag_drop_tips" to "勾选或取消勾选在快捷栏显示的功能：",
        
        // Calculator
        "history" to "计算历史",
        "clear_history" to "清除历史",
        "no_history" to "暂无计算历史记录",
        "expression_placeholder" to "输入计算表达式...",
        "click_btn_to_calc" to "点击键盘输入表达式",
        
        // Currency
        "amount" to "金额",
        "from" to "源货币",
        "to" to "目标货币",
        "convert" to "计算转换",
        "offline_warn" to "【离线模式】已加载本地缓存汇率。支持点击汇率自定义编辑。",
        "edit_rate" to "自定义汇率",
        "rate_to_usd" to "1单位该货币兑美元(USD)比例",
        "rate_edit_success" to "汇率自定义更新成功！",
        "save" to "保存",
        "cancel" to "取消",
        
        // Units
        "unit_length" to "长度换算",
        "unit_weight" to "重量换算",
        "unit_area" to "面积换算",
        "unit_volume" to "体积换算",
        "input_value" to "输入数值",
        
        // Dates
        "date_diff" to "日期相隔天数",
        "date_offset" to "日期加减推算",
        "start_date" to "开始日期",
        "end_date" to "结束日期",
        "calc_diff" to "计算相差天数",
        "diff_result" to "相差结果: ",
        "days" to "天",
        "add_days" to "增加天数",
        "sub_days" to "减少天数",
        "calc_target_date" to "计算目标日期",
        "result_date" to "目标日期: ",
        
        // Finance
        "finance_mortgage" to "房贷计算(等额本息/本金)",
        "finance_compound" to "复利理财投资计算",
        "loan_amount" to "贷款本金 (元)",
        "interest_rate" to "年利率 (%)",
        "loan_years" to "贷款期限 (年)",
        "loan_type" to "还款方式",
        "type_equal_pi" to "等额本息 (每月还款固定)",
        "type_equal_p" to "等额本金 (每月递减)",
        "calc_loan" to "计算还款计划",
        "monthly_pay" to "首月还款:",
        "total_interest" to "总利息:",
        "total_payment" to "还款总额:",
        "monthly_pay_decay" to "等额本金首月还款 %s，每月约递减 %s 元",
        
        "init_principal" to "初始本金 (元)",
        "compound_years" to "投资期限 (年)",
        "compound_freq" to "复利频率",
        "freq_annual" to "按年",
        "freq_quarterly" to "按季",
        "freq_monthly" to "按月",
        "final_balance" to "期末总资产:",
        "earned_interest" to "累计收益利息:",
        
        // Settings & Backup
        "webdav_config" to "WebDAV 数据端到端加密同步",
        "webdav_url_label" to "WebDAV 服务器地址 (如 坚果云, InfiniCLOUD 等)",
        "webdav_user_label" to "WebDAV 账户名",
        "webdav_pass_label" to "WebDAV 应用密码",
        "webdav_folder_label" to "备份目标文件夹名称",
        "webdav_encrypt_label" to "端到端加密私钥 (不设置则不加密不安全)",
        "test_conn" to "测试WebDAV连接",
        
        "backup_to_cloud" to "立即加密备份到云端",
        "restore_from_cloud" to "从云端下载并恢复覆盖",
        "conn_success" to "WebDAV 连接成功！",
        "conn_fail" to "WebDAV 连接失败，请检查配置和网络。",
        "backup_success" to "加密备份上传成功！",
        "backup_fail" to "备份失败: ",
        "restore_success" to "数据拉取并恢复重组成功！",
        "restore_fail" to "数据恢复失败: ",
        
        "local_data_title" to "本地化数据安全管理",
        "clear_local_data" to "清空本地数据库",
        "export_local_json" to "导出本地纯文本JSON备份",
        "import_local_json" to "导入文本JSON恢复",
        "data_cleared" to "本地数据已全部安全擦除!",
        "local_import_success" to "本地导入恢复成功！",
        "local_import_fail" to "本地导入解析失败！",
        "last_backup_prefix" to "上次同步时间: ",
        "never_backed_up" to "从未同步",
        "theme_setting" to "色调界面主题",
        "dark_theme" to "启用现代化深色暗黑模式",
        "lang_setting" to "多语言切换 (Language Switch)",
        "lang_current" to "当前语言: 中文繁体/简体",
        "toggle_lang_btn" to "Switch to English",
        
        // Unit units
        "unit_m" to "米 (m)", "unit_cm" to "厘米 (cm)", "unit_mm" to "毫米 (mm)", "unit_km" to "千米 (km)", "unit_inch" to "英寸 (in)", "unit_ft" to "英尺 (ft)",
        "unit_g" to "克 (g)", "unit_kg" to "千克 (kg)", "unit_oz" to "盎司 (oz)", "unit_lb" to "磅 (lb)",
        "unit_m2" to "平方米 (m²)", "unit_km2" to "平方千米 (km²)", "unit_hectare" to "公顷", "unit_acre" to "英亩",
        "unit_ml" to "毫升 (ml)", "unit_l" to "升 (L)", "unit_m3" to "立方米 (m³)", "unit_gal" to "加仑 (gal)"
    )

    private val en = mapOf(
        "app_title" to "Smart Utility Calc",
        "calc_sci" to "Sci Calc",
        "currency" to "Currency",
        "unit" to "Units",
        "date" to "Dates",
        "finance" to "Finance",
        "settings" to "Settings",
        
        "shortcut_tint" to "Long-press or click gear to customize shortcuts toolbar",
        "cust_shortcut" to "Quick-Access Shortcut Toolbar",
        "add_shortcut" to "Add to Shortcuts",
        "remove_shortcut" to "Remove from Shortcuts",
        "drag_drop_tips" to "Check elements to display on your custom quick access bar:",
        
        // Calculator
        "history" to "History",
        "clear_history" to "Clear History",
        "no_history" to "No history recorded yet",
        "expression_placeholder" to "Enter expression...",
        "click_btn_to_calc" to "Tap buttons to enter your expression",
        
        // Currency
        "amount" to "Amount",
        "from" to "From",
        "to" to "To",
        "convert" to "Convert",
        "offline_warn" to "[Offline Mode] Cached rates loaded. Click rates below to customize manually.",
        "edit_rate" to "Customize Rate",
        "rate_to_usd" to "1 unit of this currency in US Dollars (USD)",
        "rate_edit_success" to "Rate customized successfully!",
        "save" to "Save",
        "cancel" to "Cancel",
        
        // Units
        "unit_length" to "Length Converter",
        "unit_weight" to "Weight Converter",
        "unit_area" to "Area Converter",
        "unit_volume" to "Volume Converter",
        "input_value" to "Input Value",
        
        // Dates
        "date_diff" to "Days Between",
        "date_offset" to "Date Offset Calc",
        "start_date" to "Start Date",
        "end_date" to "End Date",
        "calc_diff" to "Calculate Difference",
        "diff_result" to "Total difference: ",
        "days" to "days",
        "add_days" to "Add Days",
        "sub_days" to "Subtract Days",
        "calc_target_date" to "Calculate Target Date",
        "result_date" to "Target date: ",
        
        // Finance
        "finance_mortgage" to "Mortgage Loan (Amortized/Principal)",
        "finance_compound" to "Compound Interest Calculator",
        "loan_amount" to "Principal Amount ($)",
        "interest_rate" to "Annual Rate (%)",
        "loan_years" to "Term Length (Years)",
        "loan_type" to "Repayment Method",
        "type_equal_pi" to "Amortized (Equal payments)",
        "type_equal_p" to "Equal Principal (Decaying payments)",
        "calc_loan" to "Calculate Loan",
        "monthly_pay" to "Initial payment:",
        "total_interest" to "Total Interest:",
        "total_payment" to "Total Payment:",
        "monthly_pay_decay" to "Equal principal payment: %s initially, decreases by approx %s/mo",
        
        "init_principal" to "Initial Principal ($)",
        "compound_years" to "Investment Period (Years)",
        "compound_freq" to "Compounding Frequency",
        "freq_annual" to "Annually",
        "freq_quarterly" to "Quarterly",
        "freq_monthly" to "Monthly",
        "final_balance" to "Final Balance:",
        "earned_interest" to "Earned Interest:",
        
        // Settings & Backup
        "webdav_config" to "WebDAV E2E Encrypted Synchronization",
        "webdav_url_label" to "WebDAV Server URL (e.g. jianguoyun, InfiniCLOUD)",
        "webdav_user_label" to "WebDAV Account / Mail",
        "webdav_pass_label" to "WebDAV App Password",
        "webdav_folder_label" to "Backup Folder Name",
        "webdav_encrypt_label" to "End-to-End Encryption Key (Highly Recommended)",
        "test_conn" to "Test WebDAV Connection",
        
        "backup_to_cloud" to "Encrypted Backup to Cloud Now",
        "restore_from_cloud" to "Download & Restore from Cloud",
        "conn_success" to "Connected to WebDAV successfully!",
        "conn_fail" to "Connection failed. Please check config/network.",
        "backup_success" to "Encrypted backup uploaded successfully!",
        "backup_fail" to "Backup failed: ",
        "restore_success" to "Data pulled and restored successfully!",
        "restore_fail" to "Restore failed: ",
        
        "local_data_title" to "Local Data Safe Control",
        "clear_local_data" to "Clear Local Database",
        "export_local_json" to "Export Raw JSON Backup Data",
        "import_local_json" to "Import Raw JSON to Restore",
        "data_cleared" to "Local database wiped securely!",
        "local_import_success" to "Local backup restored successfully!",
        "local_import_fail" to "Failed to parse local backup JSON!",
        "last_backup_prefix" to "Last sync: ",
        "never_backed_up" to "Never synced",
        "theme_setting" to "Visual Interface Theme",
        "dark_theme" to "Enable Modern Dark Mode Theme",
        "lang_setting" to "Language Switching Settings",
        "lang_current" to "Current Language: English",
        "toggle_lang_btn" to "切换为中文 (Switch to Chinese)",
        
        // Unit units
        "unit_m" to "Meter (m)", "unit_cm" to "Centimeter (cm)", "unit_mm" to "Millimeter (mm)", "unit_km" to "Kilometer (km)", "unit_inch" to "Inch (in)", "unit_ft" to "Feet (ft)",
        "unit_g" to "Gram (g)", "unit_kg" to "Kilogram (kg)", "unit_oz" to "Ounce (oz)", "unit_lb" to "Pound (lb)",
        "unit_m2" to "Sq Meter (m²)", "unit_km2" to "Sq Kilometer (km²)", "unit_hectare" to "Hectare", "unit_acre" to "Acre",
        "unit_ml" to "Milliliter (ml)", "unit_l" to "Liter (L)", "unit_m3" to "Cubic Meter (m³)", "unit_gal" to "Gallon (gal)"
    )

    fun get(key: String, language: String): String {
        val map = if (language.lowercase() == "zh") zh else en
        return map[key] ?: key
    }
}
