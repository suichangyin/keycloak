<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('emailCode'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post" ref="otpLoginForm">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="col-xs-12" style="display: flex; padding: 0; align-items: flex-end;">
                        <label for="emailCode" class="${properties.kcLabelClass!}">${msg("emailOtpForm")}</label>
                    </div>
                    <div class="col-xs-12" style="display: flex; padding: 0; align-items: center; gap: 8px;">
                        <div id="otp-container">
                            <input v-for="(digit, index) in codeDigits" :key="index" v-model="codeDigits[index]"
                                   class="otp-input" type="text" maxlength="1"
                                   @input="moveToNext(index)" @keydown.backspace="moveToPrev(index)"
                                   @paste="handlePaste($event)">
                        </div>
                        <input type="hidden" id="emailCode" name="emailCode" v-model="codeValue"/>
                    </div>

                    <#if messagesPerField.existsError('emailCode')>
                        <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                              aria-live="polite">
                            ${kcSanitize(messagesPerField.get('emailCode'))?no_esc}
                        </span>
                    </#if>
                    <div class="col-xs-12" style="display: flex; padding: 0; align-items: flex-end; margin-top: 20px;">
                        <p>${msg("noReceivedCode")}
                            <a href="javascript:void(0);" @click="submitForm">${msg('resendCode')}</a>
                        </p>
                        <input style="display: none" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                               name="resend" type="submit" value="${msg('resendCode')}"/>
                    </div>
                    <div class="col-xs-12" style="display: flex; padding: 0; align-items: flex-end; margin-top: 20px;">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" type="submit" value="${msg('doLogIn')}" />
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>

<style>
    #otp-container {
        display: flex;
        gap: 8px;
    }
    .otp-input {
        width: 16.6%;
        height: 40px;
        text-align: center;
        font-size: 20px;
        border: 2px solid #ccc;
        border-radius: 5px;
        outline: none;
        transition: border-color 0.2s;
    }
    .otp-input:focus {
        border-color: #007bff;
    }
</style>

<script type="text/javascript" src="${url.resourcesPath}/js/vue.min.js"></script>
<script>
    new Vue({
        el: '#kc-otp-login-form',
        data() {
            return {
                codeDigits: ["", "", "", "", "", ""], // 6个输入框
            };
        },
        computed: {
            codeValue() {
                return this.codeDigits.join(""); // 组合成完整验证码
            }
        },
        methods: {
            moveToNext(index) {
                if (this.codeDigits[index] && index < this.codeDigits.length - 1) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index + 1].focus();
                    });
                }
            },
            moveToPrev(index, event) {
                if (!this.codeDigits[index] && index > 0) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index - 1].focus();
                    });
                }
            },
            handlePaste(event) {
                event.preventDefault();
                const pastedData = event.clipboardData.getData('text').slice(0, 6).split('');
                for (let i = 0; i < pastedData.length; i++) {
                    this.codeDigits[i] = pastedData[i];
                }
            },
            submitForm() {
                const form = this.$refs.otpLoginForm;
                const resendInput = document.createElement('input');
                resendInput.type = 'hidden';
                resendInput.name = 'resend';
                resendInput.value = 'true';
                form.appendChild(resendInput);
                form.submit();
            }
        },
        mounted() {
            const form = this.$refs.otpLoginForm;
            form.addEventListener('keypress', (event) => {
                if (event.key === 'Enter') {
                    const loginButton = form.querySelector('[name="login"]');
                    if (loginButton) {
                        event.preventDefault();
                        loginButton.click();
                    }
                }
            });
        }
    });
</script>
