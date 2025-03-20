<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayWide=false; section>
    <#if section = "header">
        ${msg("configSms2Fa")}
    <#elseif section = "form">

<#--      <script src="https://cdn.jsdelivr.net/npm/vue@2.7.16/dist/vue.min.js"></script>-->
<#--      <script src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>-->
<#--      <script src="https://cdn.jsdelivr.net/npm/libphonenumber-js@1.11.9/bundle/libphonenumber-js.min.js"></script>-->
        <script type="text/javascript" src="${url.resourcesPath}/js/vue.min.js"></script>
        <script type="text/javascript" src="${url.resourcesPath}/js/axios.min.js"></script>
        <script type="text/javascript" src="${url.resourcesPath}/js/libphonenumber-mobile.js"></script>

        <div id="vue-app">
            <ul class="nav nav-tabs">
                <li :class="{'active': activeTab === 'phone'}" style="pointer-events: none; cursor: not-allowed; color: gray;" @click="activeTab = 'phone'">
                    <a href="javascript:void(0);" @click.prevent>
                        ${msg("phoneVerification")} - ${msg("disabledMessage")}
                    </a>
                </li>
                <li :class="{'active': activeTab === 'wechat'}" @click="switchToQrCode">
                    <a href="javascript:void(0);" >
                        ${msg("wechatVerification")}
                    </a>
                </li>
            </ul>

            <div class="tab-content">
                <div v-show="activeTab === 'phone'">
                    <div class="alert alert-error" v-show="errorMessage">
                        <span class="${properties.kcFeedbackErrorIcon!}"></span>
                        <span class="kc-feedback-text">{{ errorMessage }}</span>
                    </div>
                    <div id="kc-form">
                        <div id="kc-form-wrapper">
                            <form id="kc-form-login" action="${url.loginAction}" method="post">
                                <div class="${properties.kcFormGroupClass!} row" style="display: flex; align-items: flex-end;">
                                    <div class="col-xs-3" style="padding: 0 5px 0 0">
                                        <label for="countryCode" class="${properties.kcLabelClass!}">${msg("countryCode")}</label>
                                        <select v-model="selectedCountryCode" id="countryCode" name="countryCode" class="${properties.kcInputClass!}" >
                                            <option v-for="country in countries" :value="country">{{ country }} (+{{ getCountryCallingCode(country) }})</option>
                                        </select>
                                    </div>
                                    <div class="col-xs-6" style="padding: 0 5px 0 0">
                                        <label for="phoneNumber" class="${properties.kcLabelClass!}">${msg("phoneNumber")}</label>
                                        <input tabindex="1" id="phoneNumber" class="${properties.kcInputClass!}"
                                               name="phoneNumber" type="tel" <#if !phoneNumber??>autofocus</#if>
                                               value="${phoneNumber!''}" autocomplete="mobile tel"/>
                                    </div>
                                    <div class="col-xs-3" style="padding: 0 0 0 0">
                                        <input tabindex="2" style="height: 36px"
                                               class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                               v-model="sendButtonText" :disabled='sendButtonText !== initSendButtonText'
                                               v-on:click="sendVerificationCode()"
                                               type="button" value="${msg("sendVerificationCode")}"/>
                                    </div>
                                </div>
                                <div class="${properties.kcFormGroupClass!} row">
                                    <label for="code" class="${properties.kcLabelClass!}">${msg("verificationCode")}</label>
                                    <input tabindex="3" id="code" class="${properties.kcInputClass!}" name="code"
                                           type="text" <#if phoneNumber??>autofocus</#if>
                                           autocomplete="off"/>
                                </div>
                                <div class="${properties.kcFormGroupClass!} row">
                                    <input type="hidden" id="openid" name="openid" type="text"/>
                                </div>
                                <div class="${properties.kcFormGroupClass!} row">
                                    <input type="hidden" id="activeTab" name="activeTab" type="text"/>
                                </div>
                                <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                                    <input type="hidden" id="id-hidden-input" name="credentialId"
                                           <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                                    <input tabindex="5"
                                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                           :disabled='sendButtonText === initSendButtonText'
                                           name="save" id="kc-login" type="submit" value="${msg("doSubmit")}"/>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <div v-show="activeTab === 'wechat'">
                    <div v-if="qrCodeUrl" style="text-align: center">
                        <img :src="qrCodeUrl" alt="'${msg("wechat")}'" style="max-width: 70%;" @click="switchToQrCode"/>
                        <p v-if="qrCodeStatus">{{ qrCodeStatus }}</p>
                    </div>
                    <div v-else>
                        ${msg("loadingQRCode")}
                    </div>
                </div>
            </div>
        </div>

        <script type="text/javascript">
            function req(countryCode, phoneNumber) {
                const params = {params: {
                        countryCode: countryCode,
                        phoneNumber: phoneNumber
                    }
                };
                axios.get(window.location.origin + '/auth/realms/${realm.name}/sms/verification-code', params)
                    .then(res => app.disableSend(res.data.expiresIn))
                    .catch(e => app.errorMessage = e.response.data.error);
            }

            const app = new Vue({
                el: '#vue-app',
                data: {
                    activeTab: 'wechat',
                    qrCodeUrl: '',
                    qrCodeStatus: '',
                    checkQrCodeInterval: null,
                    openid: null,
                    verificationCode: '',
                    ticket: '',

                    errorMessage: '',
                    phoneNumber: '',
                    selectedCountryCode: 'CN',
                    countries: [],
                    sendButtonText: '${msg("sendVerificationCode")}',
                    initSendButtonText: '${msg("sendVerificationCode")}',
                    <#--disableSend: function (seconds) {-->
                    <#--    if (seconds <= 0) {-->
                    <#--        app.sendButtonText = app.initSendButtonText;-->
                    <#--    } else {-->
                    <#--        const minutes = Math.floor(seconds / 60) + '';-->
                    <#--        const seconds_ = seconds % 60 + '';-->
                    <#--        app.sendButtonText = String(minutes.padStart(2, '0') + ":" + seconds_.padStart(2, '0'));-->
                    <#--        setTimeout(function () {-->
                    <#--            app.disableSend(seconds - 1);-->
                    <#--        }, 1000);-->
                    <#--    }-->
                    <#--},-->
                    <#--sendVerificationCode: function () {-->
                    <#--    this.errorMessage = '';-->
                    <#--    const countryCode = document.getElementById('countryCode').value.trim();-->
                    <#--    const phoneNumber = document.getElementById('phoneNumber').value.trim();-->
                    <#--    if (!phoneNumber) {-->
                    <#--        this.errorMessage = '${msg("requiredPhoneNumber")}';-->
                    <#--        document.getElementById('phoneNumber').focus();-->
                    <#--        return;-->
                    <#--    }-->
                    <#--    if (this.sendButtonText !== this.initSendButtonText) return;-->
                    <#--    req.call(this, countryCode, '+' + getCountryCallingCode(countryCode) + phoneNumber);-->
                    <#--}-->
                },
                created() {
                    this.countries = libphonenumber.getCountries();
                },
                methods: {
                    switchToQrCode() {
                        this.activeTab = 'wechat';
                        this.fetchQrCode();
                    },
                    fetchQrCode() {
                        axios.get('/wechat/api/v1/qrcode')
                            .then(response => {
                                this.qrCodeUrl = response.data.qrCodeUrl;
                                this.ticket = response.data.ticket
                                this.startCheckingQrCodeStatus();
                            })
                            .catch(error => {
                                console.error('Error fetching QR code:', error);
                                this.errorMessage = '${msg("loadingQRCodeError")}';
                            });
                    },
                    startCheckingQrCodeStatus() {
                        this.checkQrCodeInterval = setInterval(() => {
                            axios.get('/wechat/api/v1/qrcode/status', { params: { ticket: this.ticket } })
                                .then(response => {
                                    if (response.data.status === 'scanned') {
                                        this.qrCodeStatus = '${msg("scanQRCodeSuccess")}';
                                        this.openid = response.data.openid;
                                        this.verificationCode = response.data.code;
                                        clearInterval(this.checkQrCodeInterval);
                                        this.submitWechatForm();
                                    } else if (response.data.status === 'expired') {
                                        this.qrCodeStatus = '${msg("qrcodeExpired")}';
                                        clearInterval(this.checkQrCodeInterval);
                                    } else {
                                        this.qrCodeStatus = '${msg("waitingForCodeScanning")}';
                                    }
                                })
                                .catch(error => {
                                    console.error('Error checking QR code status:', error);
                                    this.errorMessage = '${msg("failedToQueryQRCodeStatus")}';
                                    clearInterval(this.checkQrCodeInterval);
                                });
                        }, 2000);
                    },
                    submitWechatForm() {
                        document.getElementById('activeTab').value = this.activeTab;
                        document.getElementById('phoneNumber').value = this.phoneNumber;
                        document.getElementById('openid').value = this.openid;
                        document.getElementById('code').value = this.verificationCode;
                        document.getElementById('kc-form-login').submit();
                    },
                    beforeDestroy() {
                        if (this.checkQrCodeInterval) {
                            clearInterval(this.checkQrCodeInterval);
                        }
                    },
                    getCountryCallingCode(country) {
                        return libphonenumber.getCountryCallingCode(country);
                    },
                    validatePhoneNumber() {
                        const phoneNumber = this.phoneNumber.trim();
                        try {
                            const parsedPhone = libphonenumber.parsePhoneNumber(phoneNumber, this.selectedCountryCode);
                            this.isPhoneNumberValid = parsedPhone.isValid();
                            if (!this.isPhoneNumberValid) {
                                this.errorMessage = '${msg("invalidPhoneNumber")}';
                            } else {
                                this.errorMessage = '';
                            }
                        } catch (error) {
                            this.isPhoneNumberValid = false;
                            this.errorMessage = '${msg("invalidPhoneNumber")}';
                        }
                    },
                    disableSend(seconds) {
                        if (seconds <= 0) {
                            app.sendButtonText = app.initSendButtonText;
                        } else {
                            const minutes = Math.floor(seconds / 60) + '';
                            const seconds_ = seconds % 60 + '';
                            app.sendButtonText = String(minutes.padStart(2, '0') + ":" + seconds_.padStart(2, '0'));
                            setTimeout(function () {
                                app.disableSend(seconds - 1);
                            }, 1000);
                        }
                    },
                    sendVerificationCode() {
                        this.errorMessage = '';
                        const countryCode = document.getElementById('countryCode').value.trim();
                        const phoneNumber = document.getElementById('phoneNumber').value.trim();
                        if (!phoneNumber) {
                            this.errorMessage = '${msg("requiredPhoneNumber")}';
                            document.getElementById('phoneNumber').focus();
                            return;
                        }
                        if (this.sendButtonText !== this.initSendButtonText) return;
                        req.call(this, countryCode, phoneNumber);
                    },
                    getInternationalPhoneNumber(countryCode, phoneNumber) {
                        return '+' + this.getCountryCallingCode(countryCode) + phoneNumber;
                    }
                },
                mounted() {
                    this.fetchQrCode();
                }
            });
            <#if phoneNumber?? && selectedCountryCode??>
            req('${selectedCountryCode}', '${phoneNumber}');
            </#if>
        </script>
    <#elseif section = "info">
        ${msg("configSms2FaWechatInfo")}
    </#if>
</@layout.registrationLayout>
