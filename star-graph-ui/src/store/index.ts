import {defineStore} from 'pinia'
import {ref} from "vue";

export const useUserStore = defineStore('user', () => {
    const user = ref({
        isLogin: false,
        id: '',
        userName: 'Admin',
        avatar: 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif?imageView2/1/w/80/h/80'
    })

    function login(userinfo) {
        user.value.isLogin = true
        user.value.id = userinfo.id
        user.value.userName = userinfo.name
        user.value.avatar = userinfo.avatar
    }

    function logout() {
        user.value.isLogin = false
        user.value.id = ''
        user.value.userName = ''
        user.value.avatar = ''
        localStorage.setItem("accessToken",'')
        localStorage.clear()
    }

    return {
        user,
        login,
        logout
    }
}, {
    persist: {
        enabled: true,
        strategies: [
            {
                key: 'user1',
                storage: localStorage
            },
        ]
    }
})
